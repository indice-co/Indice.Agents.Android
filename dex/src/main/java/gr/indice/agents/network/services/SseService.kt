package gr.indice.agents.network.services

import gr.indice.agents.dex.models.ResultSet
import gr.indice.agents.dex.models.toBase
import gr.indice.agents.network.AgentClient
import gr.indice.agents.network.adapters.DecodingHelpers.toProblemDetails
import gr.indice.agents.network.adapters.PatchApplier
import gr.indice.agents.network.api.SseApi
import gr.indice.agents.network.models.AgentInfo
import gr.indice.agents.network.models.ChatData
import gr.indice.agents.network.models.ChatRequest
import gr.indice.agents.network.models.ConversationListItem
import gr.indice.agents.network.models.DexChatResponse
import gr.indice.agents.network.models.DexChatRole
import gr.indice.agents.network.models.GuestSession
import gr.indice.agents.network.models.LikeRequest
import gr.indice.agents.network.models.StreamData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

interface SseService {
    val statusText: StateFlow<String>
    val errorText: StateFlow<String>
    val chatData: StateFlow<List<ChatData>>

    val streamState: StateFlow<StreamData?>

    val myChatHistory: StateFlow<List<ConversationListItem>>

    suspend fun sendMessage(request: String, agentInfo: AgentInfo?)

    suspend fun getMyChats(page: Int? = null, size: Int? = null, sort: Int? = null, search: String? = null): ResultSet<ConversationListItem>

    suspend fun getChatById(chatId: String)

    suspend fun deleteChat(chatId: String)

    suspend fun likeMessage(chatId: String, messageId: String, like: Boolean?)

    fun newInstance()

}

internal class SseServiceImpl(
    private val api: SseApi = SseApi.create()
): SseService {


    private val _statusText = MutableStateFlow("")
    override val statusText = _statusText.asStateFlow()

    private val _errorText = MutableStateFlow("")
    override val errorText = _errorText.asStateFlow()

    private var conversationId: String? = null

    private var _streamState = MutableStateFlow<StreamData?>(null)
    override val streamState = _streamState.asStateFlow()
    private var hasTerminated: Boolean = false
    private var isCompleted: Boolean = false

    private val _responses = MutableStateFlow<List<ChatData>>(listOf())
    override val chatData = _responses.asStateFlow()

    private val _myChatHistory = MutableStateFlow<List<ConversationListItem>>(emptyList())
    override val myChatHistory = _myChatHistory.asStateFlow()

    private var tempMessageId: String? = null

    override suspend fun sendMessage(request: String, agentInfo: AgentInfo?) {
        withContext(Dispatchers.IO) {

            _responses.update {
                it + ChatData.UserRequest(request)
            }

            hasTerminated = false
            isCompleted = false

            val patchApplier = PatchApplier()

            val authorization = AgentClient.tokenStorage.authorization

            val responseApi = conversationId?.let {

                api.sendMessage(
                    authorization = authorization,
                    chatId = it,
                    ChatRequest(text = request, agentName = agentInfo?.name)
                )

            } ?: api.newStream(authorization = authorization, ChatRequest(text = request, agentName = agentInfo?.name))

            if (responseApi.isSuccessful) {
                val body = responseApi.body() ?: return@withContext
                body.source().use { source ->
                    if (hasTerminated) {
                        throw Exception("Received a frame after a terminal event.")
                    }

                    var id: String? = null
                    var event: String? = null
                    var retry: String? = null
                    var data: String? = null

                    var chatGuestSession: GuestSession? = null

                    while (!source.exhausted()) {
                        ensureActive()
                        val line = source.readUtf8Line() ?: break
                        when {
                            line.startsWith("id:") -> id = line.removePrefix("id:").trim()
                            line.startsWith("event:") -> event = line.removePrefix("event:").trim()
                            line.startsWith("retry:") -> retry = line.removePrefix("retry:").trim()
                            line.startsWith("data:") -> data = line.removePrefix("data:").trim()
                            line.isEmpty() -> {
                                if (data != null) {
                                    try {
                                        val it = JSONObject(data)
                                        when (val type = it.optString("type")) {
                                            "start" -> {
                                                tempMessageId = UUID.randomUUID().toString()
                                                val obj = patchApplier.parseStartData(data)
                                                if (conversationId == null || conversationId != obj.conversationId) {
                                                    conversationId = obj.conversationId

                                                    val guestSession = obj.guestSession
                                                    if (guestSession != null) {
                                                        chatGuestSession = guestSession
                                                        AgentClient.tokenStorage.parse(guestSession)
                                                    }
                                                    _streamState.value = obj.conversationId?.let { uuid ->
                                                        StreamData.Started(uuid)
                                                    }
                                                }
                                            }

                                            "error" -> {
                                                val errorText = it.optString("reason")
                                                hasTerminated = true
                                                _streamState.value = StreamData.Error(errorText)
                                                throw Exception(errorText)
                                            }

                                            else -> {
                                                if (conversationId == null)
                                                    throw Exception("Received a frame before start.")
                                                when (type) {
                                                    "status" -> {
                                                        val value = it.optString("value")
                                                        _streamState.value = StreamData.Status(value)
                                                    }

                                                    "delta" -> {
                                                        _streamState.value = StreamData.Changed
                                                        patchApplier.apply(patchApplier.parsePatch(data))

                                                        val snapshot = patchApplier.result()
                                                        snapshot.id = tempMessageId

                                                        _responses.update {
                                                            it.toMutableList().apply {
                                                                removeAll { it is ChatData.AgentResponse && it.response.id == tempMessageId }
                                                            }
                                                        }
                                                        _responses.update {
                                                            it + ChatData.AgentResponse(
                                                                snapshot
                                                            )
                                                        }
                                                    }

                                                    "done" -> {
                                                        val response = patchApplier.result()

                                                        response.id = tempMessageId
                                                        response.conversationId = conversationId
                                                        response.guestSession = chatGuestSession
                                                        response.text = response.messages
                                                            .flatMap { it.content.parts }
                                                            .joinToString("") { it.value }
                                                        _streamState.value =
                                                            StreamData.Completed(response)

                                                        _responses.update {
                                                            it.toMutableList()
                                                                .apply { removeAll { it is ChatData.AgentResponse && it.response.id == tempMessageId } }
                                                        }

                                                        _responses.update {
                                                            it + ChatData.AgentResponse(response)
                                                        }

                                                        hasTerminated = true
                                                        isCompleted = true
                                                    }

                                                    else -> {
                                                        _streamState.value = StreamData.Ignored
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println(e.stackTraceToString())
                                    }
                                }

                                id = null
                                event = null
                                retry = null
                                data = null
                            }
                        }
                    }
                    chatGuestSession = null
                }
            } else {
                _streamState.value = StreamData.Error( responseApi.errorBody()?.toProblemDetails()?.description.orEmpty())
            }

            getMyChats()
        }
    }

    override suspend fun getMyChats(
        page: Int?,
        size: Int?,
        sort: Int?,
        search: String?
    ): ResultSet<ConversationListItem> {
        val result = api.getMyChats(AgentClient.tokenStorage.authorization, page, size, sort, search)
            .takeIf { it.isSuccessful }
            ?.body()
            ?.toBase()
            ?.also {
                _myChatHistory.value = it.items.orEmpty()
            }

        return result ?: ResultSet()
    }

    override suspend fun getChatById(chatId: String) {

        val chasList = api.getChatById(authorization = AgentClient.tokenStorage.authorization, chatId = chatId).body()

        conversationId = chatId

        chasList?.let { list ->
            _responses.value = emptyList()
            val chatData = list.messages.map { message ->
                when(message.role) {
                    DexChatRole.User -> {
                        ChatData.UserRequest(message.content.parts.joinToString(" ") { it.value })
                    }
                    else -> {
                        ChatData.AgentResponse(
                            DexChatResponse(
                                conversationId = list.id,
                                usage = list.usage,
                                messages = listOf(message)
                            )
                        )
                    }
                }
            }
            _responses.value = chatData
        }
    }

    override suspend fun deleteChat(chatId: String) {
        api.deleteChat(authorization = AgentClient.tokenStorage.authorization, chatId = chatId)
        if (chatId == conversationId) {
            newInstance()
        }
        _myChatHistory.update {
            it.toMutableList().apply { removeIf { it.id == chatId } }
        }
    }

    override suspend fun likeMessage(
        chatId: String,
        messageId: String,
        like: Boolean?
    ) {
        val response = api.likeMessage(
            authorization = AgentClient.tokenStorage.authorization,
            chatId = chatId,
            messageId = messageId,
            request = LikeRequest(like)
        )

        if (response.isSuccessful) {

            _responses.update { items ->
                items.map { item ->
                    if (item is ChatData.AgentResponse &&
                        item.response.messages.any { it.messageId == messageId }
                    ) {
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
                }
            }
        }
    }

    override fun newInstance() {
        _streamState.value = null
        _responses.value = emptyList()
        conversationId = null
    }
}