package gr.indice.agents.network.models

data class AgentInfo(
    val description: String,
    val inputContentTypes: List<String>,
    val name: String,
    val outputContentTypes: List<String>,
    val author: AgentAuthor?,
    val capabilities: List<AgentCapability>?,
    val domains: List<String>?,
    val icon: String?,
    val links: List<AgentLink>?,
    val metadata: Any?,
    val tags: List<String>?
)
