package gr.indice.agents.network.models

import kotlinx.serialization.Serializable


data class ChatRequest(
    val text: String,
    val parts: List<ChatMessagePart>? = null,
    val authorName: String? = null,
    val agentName: String? = null,
    val topic: ChatTopic? = null
)

data class ChatTopic(
    val referenceId: String? = null,
    val referenceType: String? = null
)
@Serializable
data class ChatMessagePart(
    val value: String,
    val contentType: String = "text/*",
    val requestId: String? = null,
    val name: String? = null
)