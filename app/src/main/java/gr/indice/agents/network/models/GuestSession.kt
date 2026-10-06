package gr.indice.agents.network.models

import kotlinx.serialization.Serializable

@Serializable
data class GuestSession(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long? = null,
    val refreshToken: String? = null,
    val subject: String? = null
)