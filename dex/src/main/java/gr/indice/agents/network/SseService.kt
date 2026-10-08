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
import gr.indice.agents.network.models.TokenData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.util.UUID

interface SseService {
    val statusText: StateFlow<String>
    val errorText: StateFlow<String>
    val chatData: StateFlow<List<ChatData>>

    val myChatHistory: StateFlow<List<ConversationListItem>>

    suspend fun sendMessage(request: String)

    suspend fun getMyChats(page: Int? = null, size: Int? = null, sort: Int? = null, search: String? = null)

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
    private var hasTerminated: Boolean = false
    private var isCompleted: Boolean = false

    private val _responses = MutableStateFlow<List<ChatData>>(listOf())
    override val chatData = _responses.asStateFlow()

    private val _myChatHistory = MutableStateFlow<List<ConversationListItem>>(emptyList())
    override val myChatHistory = _myChatHistory.asStateFlow()

    private var tempMessageId: String? = null

    override suspend fun sendMessage(request: String) {

        AgentClient.tokenStorage.parse(object : TokenData {
            override val idToken: String?
                get() = null
            override val accessToken: String?
                get() = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjM4NDNGODY4OEZGNDM1REMxOUQ2MkU3QzQxQjRFMjQ3QURGRjg5QkVSUzI1NiIsIng1dCI6Ik9FUDRhSV8wTmR3WjFpNThRYlRpUjYzX2liNCIsInR5cCI6ImF0K2p3dCJ9.eyJpc3MiOiJodHRwczovL215LmluZGljZS5nciIsIm5iZiI6MTc5MTQ2NTc2MywiaWF0IjoxNzkxNDY1NzYzLCJleHAiOjE3OTE0NjkzNjMsImF1ZCI6ImFnZW50cyIsInNjb3BlIjoiY2hhdCIsImFtciI6WyJ1cm46aW5kaWNlOmd1ZXN0Il0sImNsaWVudF9pZCI6ImRleC1hZ2VudCIsInN1YiI6ImM5ZjdkOTEyLTYyMTQtNDNjNi1iYmM0LWQ4ZGM2MWNhNzAxNiIsImF1dGhfdGltZSI6MTc5MTQ2NTc2MywiaWRwIjoiZ3Vlc3QiLCJpcGFkZHIiOiIxNzIuMjEzLjE5Ni41MSJ9.FVwqIvFSTgkf0-I-m5ivFFJoiRFJR5g-ANxaHU7tSC3B9CRk8AyLMS_am-6TFpBV4180WfLUsezQbWvvPqMGQzmO1qDDbsQrQ55SkM-UV68VfK56EOPsUSSNGmBJayVW4G7tMuxtuoNMASMJL3yyT9--zxc8qDIp9JqAMcpCc861g3l9OpVXilnKg4_ENpr_zbIj8bX_mGqWkKOXaYOuSc6C1KvMczo1SE3QCJRXA-ewQRGfL81k_u2mPA-ZH6JAMaCwD9c5qjAG61EO7GjrQI5tQDhT3sKeZLR7jFgxB8XLqLHEJvb3COL6Uua0xedplgR-PGyhcJR9MUxLkQguAg"
            override val refreshToken: String?
                get() = null
            override val expiresIn: Long?
                get() = null
            override val tokenType: String?
                get() = "Bearer"
            override val scope: String?
                get() = null
        })

        _errorText.value = ""

        _responses.update {
            it +  ChatData.UserRequest(request)
        }

        hasTerminated = false
        isCompleted = false

        val patchApplier = PatchApplier()

        val authorization = AgentClient.tokenStorage.authorization

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

            var chatGuestSession: GuestSession? = null

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

                                            val guestSession = obj.guestSession
                                            if (guestSession != null){
                                                chatGuestSession = guestSession
                                                AgentClient.tokenStorage.parse(guestSession)
                                            }
                                            _streamState.value = conversationId?.let { uuid -> StreamData.Started(uuid) }
                                        }
                                    }
                                    "error" -> {
                                        val errorText = it.optString("reason")
                                        hasTerminated = true
                                        _statusText.value = ""
                                        _errorText.value = errorText
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
                                                response.guestSession = chatGuestSession
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
            chatGuestSession = null
        }

        getMyChats()
    }

    override suspend fun getMyChats(
        page: Int?,
        size: Int?,
        sort: Int?,
        search: String?
    ) {
        api.getMyChats(AgentClient.tokenStorage.authorization, page, size, sort, search).body()?.also {
            _myChatHistory.value = it.items
        }
    }

    override suspend fun getChatById(chatId: String) {

        val chasList = api.getChatById(authorization = AgentClient.tokenStorage.authorization, chatId = chatId).body()

        conversationId = chatId
        _statusText.value = ""
        _errorText.value = ""

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
        api.likeMessage(
            authorization = AgentClient.tokenStorage.authorization,
            chatId = chatId,
            messageId = messageId,
            request = LikeRequest(like)
        )
    }

    override fun newInstance() {
        _statusText.value = ""
        _errorText.value = ""
        _responses.value = emptyList()
        conversationId = null
    }
}