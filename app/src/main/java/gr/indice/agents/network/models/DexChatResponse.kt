package gr.indice.agents.network.models

import gr.indice.agents.network.adapters.OffsetDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class DexChatResponse(
    var id: String? = null,
    var conversationId: String? = null,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null,
    val finishReason: DexChatFinishReason? = null,
    var guestSession: GuestSession? = null,
    val limitReached: Boolean = false,
    val messages: List<DexChatMessage> = emptyList(),
    val modelId: String? = null,
    val responseId: String? = null,
    var text: String? = null,
    val usage: DexChatUsage? = null
)
