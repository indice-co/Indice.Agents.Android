package gr.indice.agents.network.models

import java.time.OffsetDateTime

data class DexConversation(
    val createdAt: OffsetDateTime,
    val id: String,
    val lastActivityAt: OffsetDateTime,
    val messageCount: Int,
    val messages: List<DexChatMessage>,
    val readOnly: Boolean,
    val title: String?,
    val usage: DexChatUsage
)
