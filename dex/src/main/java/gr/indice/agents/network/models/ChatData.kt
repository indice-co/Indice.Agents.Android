package gr.indice.agents.network.models

import java.util.UUID


sealed interface ChatData {
    val id: String

    data class AgentResponse(
        override val id: String,
        val response: DexChatResponse
    ): ChatData
    data class UserRequest(
        val question: String,
        override val id: String = UUID.randomUUID().toString()
    ): ChatData
}
