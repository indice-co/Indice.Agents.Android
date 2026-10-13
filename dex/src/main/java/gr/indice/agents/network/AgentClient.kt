package gr.indice.agents.network

import gr.indice.agents.network.models.TokenStorage
import gr.indice.agents.network.services.Service
import gr.indice.agents.network.services.ServiceInit
import okhttp3.OkHttpClient


object AgentClient {

    private var _service: Service? = null
    internal val service get() =
        _service ?: ServiceInit.create()
            .also { _service = it }

    internal val tokenStorage: TokenStorage = TokenStorage.Ephemeral()

    fun init(baseUrl: String, okHttpBuilderBlock: OkHttpClient.Builder.() -> OkHttpClient.Builder = { this }) {

        ApiClient.initialize(
            baseUrl = baseUrl,
            okHttpBuilderBlock = okHttpBuilderBlock
        )
    }

}