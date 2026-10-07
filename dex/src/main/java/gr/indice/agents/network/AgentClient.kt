package gr.indice.agents.network

import gr.indice.agents.network.models.TokenStorage
import java.util.concurrent.TimeUnit


object AgentClient {

    private var _service: Service? = null
    internal val service get() =
        _service ?: ServiceInit.create()
            .also { _service = it }

    internal lateinit var tokenStorage: TokenStorage

    fun init(baseUrl: String, tokenStorage: TokenStorage = TokenStorage.Ephemeral()) {
        ApiClient.initialize(
            baseUrl = baseUrl,
            okHttpBuilderBlock = {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(0, TimeUnit.MINUTES)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        )
        AgentClient.tokenStorage = tokenStorage
    }


}