package gr.indice.agents.network.models

import java.time.OffsetDateTime

data class ConversationListItem(
    val createdAt: OffsetDateTime,
    val id: String,
    val lastActivityAt: OffsetDateTime,
    val pin: Boolean,
    val readOnly: Boolean,
    val title: String?,
    val totalCompletionTokens: Int,
    val totalPromptTokens: Int
)
