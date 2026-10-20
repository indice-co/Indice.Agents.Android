package gr.indice.agents.dex.models

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import gr.indice.agents.dex.ui.theme.default
import gr.indice.agents.network.models.DexChatResponse


sealed interface ChatItem {
    val value: String
    val id: String
    data class UserItem(override val value: String, override val id: String): ChatItem
    data class AgentItem(override val value: String, val response: DexChatResponse, val chatContent: List<ChatContent>, override val id: String): ChatItem
}

val ChatItem.isUser: Boolean get() = this is ChatItem.UserItem

val ChatItem.shape: RoundedCornerShape get() = when(this) {
    is ChatItem.AgentItem -> {
        RoundedCornerShape(
            topStart = 2.dp,
            topEnd = default,
            bottomStart = default,
            bottomEnd = default
        )
    }
    is ChatItem.UserItem -> {
        RoundedCornerShape(
            topStart = default,
            topEnd = default,
            bottomStart = default,
            bottomEnd = 2.dp
        )
    }
}