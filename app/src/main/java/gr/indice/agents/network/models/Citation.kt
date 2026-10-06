package gr.indice.agents.network.models

import kotlinx.serialization.Serializable

@Serializable
data class Citation(
    val chunkId: String,
    val documentId: String,
    val headingPath: String? = null,
    val number: Int,
    val score: Double,
    val snippet: String? = null,
    val sourceUrl: String? = null,
    val title: String? = null
)
