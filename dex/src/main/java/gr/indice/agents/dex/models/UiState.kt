package gr.indice.agents.dex.models

import gr.indice.agents.network.models.AgentInfo
import gr.indice.agents.network.models.ChatPhase
import gr.indice.agents.network.models.DexChatUsage

data class UiState(
    val isInitializing: Boolean = true,
    val agents: List<AgentInfo> = emptyList(),
    val selectedAgent: AgentInfo? = null,
    val chat: List<ChatItem> = emptyList(),
    val usage: DexChatUsage? = null,
    val phase: ChatPhase = ChatPhase.Idle
) {
    val isBusy: Boolean get() = phase is ChatPhase.Loading || phase is ChatPhase.Streaming
}
