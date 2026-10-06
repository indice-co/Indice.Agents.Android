package gr.indice.agents.network

import gr.indice.agents.network.adapters.PatchApplier
import gr.indice.agents.network.api.SseApi
import gr.indice.agents.network.models.ChatData
import gr.indice.agents.network.models.ChatRequest
import gr.indice.agents.network.models.ConversationListItem
import gr.indice.agents.network.models.DexChatResponse
import gr.indice.agents.network.models.DexChatRole
import gr.indice.agents.network.models.GuestSession
import gr.indice.agents.network.models.LikeRequest
import gr.indice.agents.network.models.StreamData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.util.UUID
import kotlin.collections.toMutableList

interface SseService {
    val statusText: StateFlow<String>
    val chatData: StateFlow<List<ChatData>>

    val myChatHistory: StateFlow<List<ConversationListItem>>

    suspend fun sendMessage(request: String)

    suspend fun getMyChats(page: Int? = null, size: Int? = null, sort: Int? = null, search: String? = null)

    suspend fun getChatById(chatId: String)

    suspend fun deleteChat(chatId: String)

    suspend fun likeMessage(chatId: String, messageId: String, like: Boolean?)

    fun newInstance()

}

internal class SseServiceImpl(private val api: SseApi = SseApi.create()): SseService {


    private val _statusText = MutableStateFlow("")


    override val statusText = _statusText.asStateFlow()

    private var conversationId: String? = null
    private var guestSession: GuestSession? = null

    private var _streamState = MutableStateFlow<StreamData?>(null)
    private var hasTerminated: Boolean = false
    private var isCompleted: Boolean = false

    private val _responses = MutableStateFlow<List<ChatData>>(listOf())
    override val chatData = _responses.asStateFlow()

    private val _myChatHistory = MutableStateFlow<List<ConversationListItem>>(emptyList())
    override val myChatHistory = _myChatHistory.asStateFlow()

    private var tempMessageId: String? = null

    override suspend fun sendMessage(request: String) {

        _responses.update {
            it +  ChatData.UserRequest(request)
        }

        hasTerminated = false
        isCompleted = false

        val patchApplier = PatchApplier()

        val authorization = guestSession?.let { "${it.tokenType} ${it.accessToken}" }

        val responseApi = conversationId?.takeIf { authorization != null }?.let {

            api.sendMessage(
                authorization = authorization!!,
                chatId = it,
                ChatRequest(text = request)
            )

        } ?: api.newStream(authorization = authorization, ChatRequest(text = request))

        val body = responseApi.body() ?: return



        body.source().use { source ->
            if (hasTerminated) { throw Exception("Received a frame after a terminal event.") }

            var id    : String? = null
            var event : String? = null
            var retry : String? = null
            var data  : String? = null

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith("id:")      -> id    = line.removePrefix("id:").trim()
                    line.startsWith("event:")   -> event = line.removePrefix("event:").trim()
                    line.startsWith("retry:")   -> retry = line.removePrefix("retry:").trim()
                    line.startsWith("data:")    -> data  = line.removePrefix("data:").trim()
                    line.isEmpty() -> {
                        if (data != null) {
                            try {
                                val it = JSONObject(data)
                                when(val type = it.optString("type")) {
                                    "start" -> {
                                        tempMessageId = UUID.randomUUID().toString()
                                        val obj = patchApplier.parseStartData(data)
                                        if (conversationId == null || conversationId != obj.conversationId) {
                                            conversationId = obj.conversationId
                                            if (obj.guestSession != null){
                                                guestSession = obj.guestSession
                                            }
                                            _streamState.value = conversationId?.let { uuid -> StreamData.Started(uuid) }
                                        }
                                    }
                                    "error" -> {
                                        hasTerminated = true
                                        val errorText = it.optString("reason")
                                        throw Exception(errorText)
                                    }
                                    else -> {
                                        if (conversationId == null)
                                            throw Exception("Received a frame before start.")
                                        when(type) {
                                            "status" -> {
                                                val value = it.optString("value")
                                                _statusText.value = value
                                                _streamState.value = StreamData.Status(value)
                                            }
                                            "delta"  -> {
                                                _statusText.value = ""
                                                _streamState.value = StreamData.Changed
                                                patchApplier.apply(patchApplier.parsePatch(data))

                                                val snapshot = patchApplier.result()
                                                snapshot.id = tempMessageId

                                                _responses.update {
                                                    it.toMutableList().apply {
                                                        removeAll { it is ChatData.AgentResponse && it.response.id == tempMessageId  }
                                                    }
                                                }
                                                _responses.update { it + ChatData.AgentResponse(snapshot) }
                                            }

                                            "done"  -> {
                                                val response = patchApplier.result()

                                                response.id = tempMessageId
                                                response.conversationId = conversationId
                                                response.guestSession = guestSession
                                                response.text = response.messages
                                                    .flatMap { it.content.parts }
                                                    .joinToString("") { it.value }
                                                _streamState.value = StreamData.Completed(response)

                                                _responses.update {
                                                    it.toMutableList().apply { removeAll { it is ChatData.AgentResponse && it.response.id == tempMessageId  } }
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
        }

        getMyChats()
    }

    override suspend fun getMyChats(
        page: Int?,
        size: Int?,
        sort: Int?,
        search: String?
    ) {
        guestSession?.let {
            val authorization = "${it.tokenType} ${it.accessToken}"
            api.getMyChats(authorization, page, size, sort, search).body()?.also {
                _myChatHistory.value = it.items
            }
        }
    }

    override suspend fun getChatById(chatId: String) {
        guestSession?.let {
            val authorization = "${it.tokenType} ${it.accessToken}"
            val chasList = api.getChatById(authorization =  authorization, chatId = chatId).body()

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
                                    messages = listOf(message)
                                )
                            )
                        }
                    }
                }
                _responses.value = chatData
            }

        } ?: throw Exception("No user found")
    }

    override suspend fun deleteChat(chatId: String) {
        guestSession?.let {
            val authorization = "${it.tokenType} ${it.accessToken}"
            api.deleteChat(authorization =  authorization, chatId = chatId)
            if (chatId == conversationId) {
                newInstance()
            }
            _myChatHistory.update {
                it.toMutableList().apply { removeIf { it.id == chatId } }
            }
        } ?: throw Exception("No user found")
    }

    override suspend fun likeMessage(
        chatId: String,
        messageId: String,
        like: Boolean?
    ) {
        guestSession?.let {
            val authorization = "${it.tokenType} ${it.accessToken}"
            api.likeMessage(authorization =  authorization, chatId = chatId, messageId = messageId, request = LikeRequest(
                like
            )
            )
        } ?: throw Exception("No user found")
    }

    override fun newInstance() {
        _responses.value = emptyList()
        conversationId = null
    }
}