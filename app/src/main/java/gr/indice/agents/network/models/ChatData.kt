package gr.indice.agents.network.models


sealed interface ChatData {
    data class AgentResponse(
        val response: DexChatResponse
    ): ChatData
    data class UserRequest(
        val question: String
    ): ChatData
}