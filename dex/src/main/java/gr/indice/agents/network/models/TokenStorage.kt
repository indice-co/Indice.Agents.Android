package gr.indice.agents.network.models

interface TokenStorageAccessor {
    val tokenType: String?
    val idToken: String?
    val accessToken: String?
    val refreshToken: String?
    val authorization: String?
}

interface TokenStorage: TokenStorageAccessor {

    companion object

    fun parse(tokenResponse: TokenData)
    fun clear()

    class Ephemeral: TokenStorage {

        override fun parse(tokenResponse: TokenData) {
            tokenType = tokenResponse.tokenType
            idToken = tokenResponse.idToken
            accessToken = tokenResponse.accessToken
            refreshToken = tokenResponse.refreshToken
        }

        override fun clear() {
            tokenType = null
            idToken = null
            accessToken = null
            refreshToken = null
        }

        override var tokenType: String? = null
            private set
        override var idToken: String? = null
            private set
        override var accessToken: String? = null
            private set
        override var refreshToken: String? = null
            private set

        override val authorization: String? get() {
            val type = tokenType ?: return null
            val access = accessToken ?: return null

            return "$type $access"
        }
    }
}