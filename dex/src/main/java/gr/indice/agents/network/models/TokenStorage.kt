package gr.indice.agents.network.models

import java.util.concurrent.atomic.AtomicReference

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

    fun clear(authorization: String)

    class Ephemeral: TokenStorage {

        private data class Snapshot(
            val tokenType: String?,
            val idToken: String?,
            val accessToken: String?,
            val refreshToken: String?
        ) {
            val authorization: String? get() {
                val type = tokenType ?: return null
                val access = accessToken ?: return null

                return "$type $access"
            }
        }

        private val snapshot = AtomicReference<Snapshot?>(null)

        override fun parse(tokenResponse: TokenData) {
            snapshot.set(
                Snapshot(
                    tokenType = tokenResponse.tokenType,
                    idToken = tokenResponse.idToken,
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken
                )
            )
        }

        override fun clear() {
            snapshot.set(null)
        }

        override fun clear(authorization: String) {
            snapshot.updateAndGet { current ->
                current?.takeIf { it.authorization != authorization }
            }
        }

        override val tokenType: String? get() = snapshot.get()?.tokenType
        override val idToken: String? get() = snapshot.get()?.idToken
        override val accessToken: String? get() = snapshot.get()?.accessToken
        override val refreshToken: String? get() = snapshot.get()?.refreshToken
        override val authorization: String? get() = snapshot.get()?.authorization
    }
}
