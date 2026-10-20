package gr.indice.agents.dex

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.filter
import gr.indice.agents.dex.models.ChatItem
import gr.indice.agents.dex.models.UiState
import gr.indice.agents.dex.utilities.ChatContentMapper
import gr.indice.agents.dex.utilities.Pager
import gr.indice.agents.network.AgentClient
import gr.indice.agents.network.models.AgentInfo
import gr.indice.agents.network.models.ChatData
import gr.indice.agents.network.models.ConversationListItem
import gr.indice.agents.network.services.AgentsService
import gr.indice.agents.network.services.SseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class AgentUiViewModel(
    private val sseService: SseService = AgentClient.service.sseService,
    private val agentsService: AgentsService = AgentClient.service.agentsService,
): ViewModel() {

    private var job: Job? = null

    private val selectedAgent = MutableStateFlow<AgentInfo?>(null)
    private val isInitializing = MutableStateFlow(true)

    /** Mapped items by ChatData id, so only changed messages are parsed again. */
    private var chatCache: Map<String, Pair<ChatData, ChatItem>> = emptyMap()

    val uiState: StateFlow<UiState> = combine(
        sseService.state,
        agentsService.agents,
        selectedAgent,
        isInitializing
    ) { session, agents, selected, initializing ->
        UiState(
            isInitializing = initializing,
            agents = agents,
            selectedAgent = selected?.takeIf { it in agents } ?: agents.firstOrNull(),
            chat = mapChat(session.messages),
            usage = session.usage,
            phase = session.phase
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    val searchText = TextFieldState()

    private val conversationPager = Pager<ConversationListItem>(hasLocalFilter = false) { page, size ->
        sseService.getMyChats(page = page, size = size)
    }

    private val filterProvider = snapshotFlow { searchText.text.toString() }
        .map { search ->
            conversationPager.hasLocalFilter = search.isNotEmpty()
            if (search.isEmpty()) {
                null
            } else {
                { item: ConversationListItem -> item.title?.contains(search, ignoreCase = true) == true }
            }
        }

    val conversationPagerFlow = conversationPager
        .getPagerFlow()
        .cachedIn(viewModelScope)
        .combine(filterProvider) { page, filter ->
            if (filter == null) page else page.filter(filter)
        }

    init {
        viewModelScope.launch {
            agentsService.getAgents()
            isInitializing.value = false
        }
    }

    private fun mapChat(messages: List<ChatData>): List<ChatItem> {
        val previous = chatCache
        val next = HashMap<String, Pair<ChatData, ChatItem>>(messages.size)

        val items = messages.map { data ->
            val cached = previous[data.id]
            val item = if (cached != null && cached.first === data) cached.second else data.toChatItem(cached)
            next[data.id] = data to item
            item
        }

        chatCache = next
        return items.filter { it.value.isNotBlank() }
    }

    private fun ChatData.toChatItem(previous: Pair<ChatData, ChatItem>?): ChatItem = when (this) {
        is ChatData.UserRequest -> ChatItem.UserItem(value = question, id = id)
        is ChatData.AgentResponse -> {
            val messages = response.messages
            val previousMessages = (previous?.first as? ChatData.AgentResponse)?.response?.messages.orEmpty()
            val previousContent = (previous?.second as? ChatItem.AgentItem)?.chatContent.orEmpty()

            val chatContent = if (messages.size == 1 && previousMessages.size == 1) {
                ChatContentMapper.items(messages[0].content, previousMessages[0].content.parts, previousContent)
            } else {
                messages.flatMap { ChatContentMapper.items(it.content) }
            }

            ChatItem.AgentItem(
                value = messages.flatMap { it.content.parts }.joinToString(" ") { it.value },
                response = response,
                chatContent = chatContent,
                id = id
            )
        }
    }

    private fun refreshHistory() {
        conversationPager.invalidate()
    }

    fun likeMessage(chatId: String, messageId: String, like: Boolean?) {
        viewModelScope.launch {
            sseService.likeMessage(chatId, messageId, like)
        }
    }

    fun deleteChatHistory(id: String) {
        viewModelScope.launch {
            val isCurrent = sseService.state.value.conversationId == id
            if (sseService.deleteConversation(id)) {
                if (isCurrent) stop()
                refreshHistory()
            }
        }
    }

    fun loadFromHistory(id: String) {
        job?.cancel()
        job = viewModelScope.launch {
            sseService.loadConversation(id)
        }
    }

    fun newChat() {
        stop()
        sseService.reset()
    }

    fun selectedAgent(agent: AgentInfo) {
        selectedAgent.value = agent
    }

    fun ask(question: String) {
        if (job?.isActive == true) return

        val agent = selectedAgent.value ?: agentsService.agents.value.firstOrNull()
        job = viewModelScope.launch {
            sseService.sendMessage(question, agent)
            refreshHistory()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    override fun onCleared() {
        stop()
        sseService.reset()
    }
}
