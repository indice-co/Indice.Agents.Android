package gr.indice.agents.network.models

data class ChatSessionState(
    val conversationId: String? = null,
    val messages: List<ChatData> = emptyList(),
    val usage: DexChatUsage? = null,
    val phase: ChatPhase = ChatPhase.Idle
)

sealed interface ChatPhase {
    data object Idle: ChatPhase
    data object Loading: ChatPhase
    data class Streaming(val status: String?): ChatPhase
    data class Error(val message: String): ChatPhase
}
