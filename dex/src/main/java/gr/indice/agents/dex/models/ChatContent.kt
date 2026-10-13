package gr.indice.agents.dex.models

data class ChatContent(
    val id: Int,
    val content: ChatContentType,
    val caption: String?
)
