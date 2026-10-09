package gr.indice.agents.network

import gr.indice.agents.network.models.TokenStorage
import java.util.concurrent.TimeUnit


object AgentClient {

    private var _service: Service? = null
    internal val service get() =
        _service ?: ServiceInit.create()
            .also { _service = it }

    internal val tokenStorage: TokenStorage = TokenStorage.Ephemeral()

    fun init(baseUrl: String) {
        ApiClient.initialize(
            baseUrl = baseUrl,
            okHttpBuilderBlock = {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(0, TimeUnit.MINUTES)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        )
    }

}