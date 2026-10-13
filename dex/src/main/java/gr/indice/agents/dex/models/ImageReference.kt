package gr.indice.agents.dex.models

import kotlinx.serialization.Serializable

@Serializable
data class ImageReference(
    val uri: String? = null,
    val url: String? = null,
    val caption: String? = null,
    val alt: String? = null,
)