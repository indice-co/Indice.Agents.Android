package gr.indice.agents.dex.models

import gr.indice.agents.network.models.ConversationListItemResult


data class ResultSet<T: Any>(
    val count: Int? = null,
    val items: List<T>? = null
)

fun ConversationListItemResult.toBase() = ResultSet(count = this.count, items = this.items)