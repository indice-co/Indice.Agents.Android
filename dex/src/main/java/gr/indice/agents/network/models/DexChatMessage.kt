package gr.indice.agents.network.models

import gr.indice.agents.network.adapters.OffsetDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class DexChatMessage(
    val authorName: String? = null,
    val citations: List<Citation> = emptyList(),
    val content: ChatMessageContent,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null,
    val liked: Boolean? = null,
    val messageId: String? = null,
    val role: DexChatRole
)
