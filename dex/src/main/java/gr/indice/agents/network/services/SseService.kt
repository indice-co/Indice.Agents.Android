package gr.indice.agents.network.services

import gr.indice.agents.dex.BuildConfig
import gr.indice.agents.dex.models.ResultSet
import gr.indice.agents.dex.models.toBase
import gr.indice.agents.network.AgentClient
import gr.indice.agents.network.adapters.DecodingHelpers.toProblemDetails
import gr.indice.agents.network.adapters.PatchApplier
import gr.indice.agents.network.api.SseApi
import gr.indice.agents.network.models.AgentInfo
import gr.indice.agents.network.models.ChatData
import gr.indice.agents.network.models.ChatPhase
import gr.indice.agents.network.models.ChatRequest
import gr.indice.agents.network.models.ChatSessionState
import gr.indice.agents.network.models.ConversationListItem
import gr.indice.agents.network.models.DexChatResponse
import gr.indice.agents.network.models.DexChatRole
import gr.indice.agents.network.models.DexConversation
import gr.indice.agents.network.models.LikeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

interface SseService {
    val state: StateFlow<ChatSessionState>

    suspend fun sendMessage(request: String, agentInfo: AgentInfo?)

    suspend fun loadConversation(chatId: String)

    suspend fun deleteConversation(chatId: String): Boolean

    suspend fun likeMessage(chatId: String, messageId: String, like: Boolean?)

    suspend fun getMyChats(page: Int? = null, size: Int? = null, sort: Int? = null, search: String? = null): ResultSet<ConversationListItem>

    fun reset()
}

internal class SseServiceImpl(
    private val api: SseApi = SseApi.create()
): SseService {

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(ChatSessionState())
    override val state = _state.asStateFlow()


    private val generation = AtomicLong(0)

    private class StreamContext(val session: Long, val pendingId: String) {
        val patchApplier = PatchApplier()
        var started = false
    }

    override suspend fun sendMessage(request: String, agentInfo: AgentInfo?) {
        withContext(Dispatchers.IO) {
            val stream = StreamContext(session = generation.get(), pendingId = UUID.randomUUID().toString())
            val chatRequest = ChatRequest(text = request, agentName = agentInfo?.name)

            updateSession(stream.session) {
                it.copy(messages = it.messages + ChatData.UserRequest(request), phase = ChatPhase.Loading)
            }

            try {
                val response = _state.value.conversationId
                    ?.let { api.sendMessage(chatId = it, body = chatRequest) }
                    ?: api.newStream(body = chatRequest)

                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    val message = response.errorBody()?.toProblemDetails()?.description ?: GENERIC_ERROR
                    updateSession(stream.session) { it.copy(phase = ChatPhase.Error(message)) }
                    return@withContext
                }

                val closeOnCancel = coroutineContext.job.invokeOnCompletion { body.close() }
                try {
                    body.use { readStream(it, stream) }
                } finally {
                    closeOnCancel.dispose()
                }
            } catch (e: Exception) {
                if (e is CancellationException || !isActive) {
                    updateSession(stream.session) { it.withoutPending(stream.pendingId).copy(phase = ChatPhase.Idle) }
                    ensureActive()
                    throw e
                }
                if (BuildConfig.DEBUG) e.printStackTrace()
                updateSession(stream.session) {
                    it.withoutPending(stream.pendingId).copy(phase = ChatPhase.Error(e.message ?: GENERIC_ERROR))
                }
            }
        }
    }

    private fun CoroutineScope.readStream(body: ResponseBody, stream: StreamContext) {
        val source = body.source()
        var data: String? = null

        while (true) {
            ensureActive()
            val line = source.readUtf8Line() ?: break
            when {
                line.startsWith("data:") -> {
                    val value = line.removePrefix("data:").trim()
                    data = data?.let { "$it\n$value" } ?: value
                }
                line.isEmpty() -> {
                    val frame = data
                    data = null
                    if (frame != null && stream.handle(frame)) return
                }
            }
        }

        updateSession(stream.session) {
            it.withoutPending(stream.pendingId).copy(phase = ChatPhase.Error(STREAM_ENDED_ERROR))
        }
    }

    private fun StreamContext.handle(data: String): Boolean {
        val frame = try {
            json.parseToJsonElement(data).jsonObject
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
            return false
        }

        val type = frame["type"]?.jsonPrimitive?.contentOrNull

        when {
            type == "start" -> {
                val start = patchApplier.parseStartData(frame)
                start.guestSession?.let(AgentClient.tokenStorage::parse)
                started = true
                updateSession(session) {
                    it.copy(conversationId = start.conversationId ?: it.conversationId, phase = ChatPhase.Streaming(null))
                }
            }

            type == "error" -> {
                val reason = frame["reason"]?.jsonPrimitive?.contentOrNull ?: GENERIC_ERROR
                updateSession(session) { it.withoutPending(pendingId).copy(phase = ChatPhase.Error(reason)) }
                return true
            }

            !started -> if (BuildConfig.DEBUG) println("Ignored '$type' frame received before start.")

            type == "status" -> {
                val status = frame["value"]?.jsonPrimitive?.contentOrNull
                updateSession(session) { it.copy(phase = ChatPhase.Streaming(status)) }
            }

            type == "delta" -> {
                try {
                    patchApplier.apply(patchApplier.parsePatch(frame))
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) e.printStackTrace()
                    return false
                }
                val snapshot = patchApplier.result()
                updateSession(session) { state ->
                    state.upsertAgent(pendingId, snapshot.copy(id = pendingId, conversationId = state.conversationId))
                        .copy(usage = snapshot.usage ?: state.usage, phase = ChatPhase.Streaming(null))
                }
            }

            type == "done" -> {
                val response = patchApplier.result()
                updateSession(session) { state ->
                    val final = response.copy(
                        id = pendingId,
                        conversationId = state.conversationId,
                        text = response.messages.flatMap { it.content.parts }.joinToString("") { it.value }
                    )
                    state.upsertAgent(pendingId, final)
                        .copy(usage = response.usage ?: state.usage, phase = ChatPhase.Idle)
                }
                return true
            }

            else -> if (BuildConfig.DEBUG) println("Ignored unknown '$type' frame.")
        }
        return false
    }

    override suspend fun loadConversation(chatId: String) {
        withContext(Dispatchers.IO) {
            val session = generation.incrementAndGet()
            _state.value = ChatSessionState(phase = ChatPhase.Loading)

            val conversation = safeCall { api.getChatById(chatId = chatId) }
                ?.takeIf { it.isSuccessful }
                ?.body()

            updateSession(session) {
                conversation?.toSessionState()
                    ?: ChatSessionState(phase = ChatPhase.Error(LOAD_ERROR))
            }
        }
    }

    override suspend fun deleteConversation(chatId: String): Boolean =
        withContext(Dispatchers.IO) {
            val deleted = safeCall { api.deleteChat(chatId = chatId) }?.isSuccessful == true
            if (deleted && _state.value.conversationId == chatId) {
                reset()
            }
            deleted
        }

    override suspend fun likeMessage(chatId: String, messageId: String, like: Boolean?) {
        withContext(Dispatchers.IO) {
            val response = safeCall {
                api.likeMessage(chatId = chatId, messageId = messageId, request = LikeRequest(like))
            }
            if (response?.isSuccessful != true) return@withContext

            _state.update { state ->
                state.copy(messages = state.messages.map { item ->
                    if (item is ChatData.AgentResponse && item.response.messages.any { it.messageId == messageId }) {
                        item.copy(
                            response = item.response.copy(
                                messages = item.response.messages.map { msg ->
                                    if (msg.messageId == messageId) msg.copy(liked = like) else msg
                                }
                            )
                        )
                    } else {
                        item
                    }
                })
            }
        }
    }

    override suspend fun getMyChats(
        page: Int?,
        size: Int?,
        sort: Int?,
        search: String?
    ): ResultSet<ConversationListItem> =
        withContext(Dispatchers.IO) {
            safeCall { api.getMyChats(page, size, sort, search) }
                ?.takeIf { it.isSuccessful }
                ?.body()
                ?.toBase()
                ?: ResultSet()
        }

    override fun reset() {
        generation.incrementAndGet()
        _state.value = ChatSessionState()
    }

    private inline fun updateSession(session: Long, transform: (ChatSessionState) -> ChatSessionState) {
        _state.update { if (generation.get() == session) transform(it) else it }
    }

    private fun ChatSessionState.withoutPending(pendingId: String) =
        copy(messages = messages.filterNot { it is ChatData.AgentResponse && it.id == pendingId })

    private fun ChatSessionState.upsertAgent(pendingId: String, response: DexChatResponse): ChatSessionState {
        val item = ChatData.AgentResponse(id = pendingId, response = response)
        val index = messages.indexOfLast { it is ChatData.AgentResponse && it.id == pendingId }
        val updated = if (index >= 0) {
            messages.toMutableList().apply { set(index, item) }
        } else {
            messages + item
        }
        return copy(messages = updated)
    }

    private fun DexConversation.toSessionState() = ChatSessionState(
        conversationId = id,
        usage = usage,
        messages = messages.map { message ->
            val itemId = message.messageId ?: UUID.randomUUID().toString()
            when (message.role) {
                DexChatRole.User -> ChatData.UserRequest(
                    question = message.content.parts.joinToString(" ") { it.value },
                    id = itemId
                )
                else -> ChatData.AgentResponse(
                    id = itemId,
                    response = DexChatResponse(
                        id = itemId,
                        conversationId = id,
                        usage = usage,
                        messages = listOf(message)
                    )
                )
            }
        }
    )

    private companion object {
        const val GENERIC_ERROR = "Something went wrong"
        const val LOAD_ERROR = "The conversation could not be loaded"
        const val STREAM_ENDED_ERROR = "The response ended unexpectedly"
    }
}
