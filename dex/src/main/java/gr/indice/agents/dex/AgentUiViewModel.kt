package gr.indice.agents.dex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gr.indice.agents.dex.models.ChatItem
import gr.indice.agents.dex.models.UiState
import gr.indice.agents.dex.utilities.ChatContentMapper
import gr.indice.agents.network.AgentClient
import gr.indice.agents.network.models.AgentInfo
import gr.indice.agents.network.models.ChatData
import gr.indice.agents.network.models.DexChatUsage
import gr.indice.agents.network.services.AgentsService
import gr.indice.agents.network.services.SseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AgentUiViewModel(
    private val sseService: SseService = AgentClient.service.sseService,
    private val agentsService: AgentsService = AgentClient.service.agentsService,
): ViewModel() {

    private var job: Job? = null
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _sessionQuestionsLimit = MutableStateFlow<DexChatUsage?>(null)
    val sessionQuestionsLimit = _sessionQuestionsLimit.asStateFlow()
    val myChats get() = sseService.myChatHistory
    val errorMessage get () = sseService.errorText

    val agents get() = agentsService.agents

    private var selectedAgent: AgentInfo? = null

    val chatList = sseService.chatData
        .map {
            it.map { chatData ->
                when(chatData) {
                    is ChatData.AgentResponse -> {
                        ChatItem.AgentItem(
                            value = chatData.response.messages
                                .flatMap { it.content.parts }
                                .joinToString(" ") { it.value },
                            response = chatData.response,
                            chatContent = chatData.response.messages
                                .flatMap { ChatContentMapper.items(it.content) }
                        ).also {
                            chatData.response.usage?.let { usage ->
                                _sessionQuestionsLimit.value = usage
                            }
                        }
                    }
                    is ChatData.UserRequest -> {
                        ChatItem.UserItem(chatData.question)
                    }
                }
            }.filter {
                it.value.isNotBlank()
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        viewModelScope.launch {
            sseService.statusText.collect { status ->
                _uiState.update { it.copy(statusText = status, isLoading = false) }
            }
        }
        viewModelScope.launch {
            agentsService.getAgents()
            sseService.getMyChats()
        }

        viewModelScope.launch {
            agents.collectLatest {
                selectedAgent = it.firstOrNull()
            }
        }
    }

    fun likeMessage(chatId: String, messageId: String, like: Boolean?) {
        viewModelScope.launch(Dispatchers.IO) {
            sseService.likeMessage(chatId, messageId, like)
        }
    }

    fun deleteChatHistory(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sseService.deleteChat(id)
        }
    }

    fun loadFromHistory(id: String) {
        stop()
        viewModelScope.launch(Dispatchers.IO) {
            sseService.getChatById(id)
        }
    }

    fun newChat() {
        sseService.newInstance()
        _sessionQuestionsLimit.value = null
        _uiState.update {
            it.copy(statusText = "", isLoading = false)
        }
    }

    fun selectedAgent(agent: AgentInfo) {
        selectedAgent = agent
    }

    fun ask(question: String) {
        if (job?.isActive == true) return

        job = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isConnected = true, isLoading = true) }
            sseService.sendMessage(question, selectedAgent)
            _uiState.update { it.copy(isConnected = false, isLoading = false) }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _uiState.update { it.copy(isConnected = false, isLoading = false, statusText = "") }
    }
}