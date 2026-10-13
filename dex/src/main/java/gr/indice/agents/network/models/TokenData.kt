package gr.indice.agents.network.models

interface TokenData {
    val idToken: String?
    val accessToken: String?
    val refreshToken: String?
    val expiresIn: Long?
    val tokenType: String?
    val scope: String?
}