package gr.indice.agents.network.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageContent(
    val parts: List<ChatMessagePart> = emptyList()
)
