package gr.indice.agents.dex

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gr.indice.agents.dex.models.ChatItem
import gr.indice.agents.dex.models.UiState
import gr.indice.agents.dex.utilities.ChatContentMapper
import gr.indice.agents.network.AgentClient
import gr.indice.agents.network.SseService
import gr.indice.agents.network.models.ChatData
import gr.indice.agents.network.models.DexChatUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
class AgentUiViewModel(
    private val service: SseService = AgentClient.service.sseService
): ViewModel() {

    private var job: Job? = null
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _sessionQuestionsLimit = MutableStateFlow<DexChatUsage?>(null)
    val sessionQuestionsLimit = _sessionQuestionsLimit.asStateFlow()
    val myChats = service.myChatHistory
    val errorMessage = service.errorText

    val chatList = service.chatData
        .map {
        it.map { chatData ->
            when(chatData) {
                is ChatData.AgentResponse -> {

                    chatData.response.usage?.let { usage ->
                        _sessionQuestionsLimit.value = usage
                    }

                    ChatItem.AgentItem(
                        value = chatData.response.messages
                            .flatMap { it.content.parts }
                            .joinToString(" ") { it.value },
                        response = chatData.response,
                        chatContent = chatData.response.messages
                            .flatMap { ChatContentMapper.items(it.content) }
                    )
                }
                is ChatData.UserRequest -> {
                    ChatItem.UserItem(chatData.question)
                }
            }
        }.filter {
            it.value.isNotBlank()
        }

    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        viewModelScope.launch {
            service.statusText.collect { status ->
                _uiState.update { it.copy(statusText = status) }
            }
        }
    }

    fun likeMessage(chatId: String, messageId: String, like: Boolean?) {
        viewModelScope.launch(Dispatchers.IO) {
            service.likeMessage(chatId, messageId, like)
        }
    }

    fun deleteChatHistory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            service.deleteChat(id)
        }
    }

    fun loadFromHistory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            service.getChatById(id)
        }
    }

    fun newChat() {
        service.newInstance()
        _sessionQuestionsLimit.value = null
    }

    fun ask(question: String) {
        if (job?.isActive == true) return
        _uiState.value = UiState(isConnected = true)

        job = viewModelScope.launch(Dispatchers.IO) {
            service.sendMessage(question)
            _uiState.update { it.copy(isConnected = false) }
        }
    }

    fun stop() {
        job?.cancel()
        _uiState.update { it.copy(isConnected = false) }
    }
}