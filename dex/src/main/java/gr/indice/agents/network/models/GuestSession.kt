package gr.indice.agents.network.models

import kotlinx.serialization.Serializable

@Serializable
data class GuestSession(
    override val accessToken: String,
    override val tokenType: String,
    override val expiresIn: Long? = null,
    override val refreshToken: String? = null,
    override val scope: String? = null,
    override val idToken: String? = null,
    val subject: String? = null
): TokenData