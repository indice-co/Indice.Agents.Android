package gr.indice.agents.network.models


data class Event(
    val type: String?,
    val conversationId: String? = null,
    val guestSession: GuestSession? = null,
    val value: String? = null,
    val reason: String? = null
)