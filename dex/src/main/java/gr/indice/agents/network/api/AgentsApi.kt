package gr.indice.agents.network.api

import gr.indice.agents.network.ApiClient
import gr.indice.agents.network.models.AgentInfo
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET

internal interface AgentsApi {

    @GET("api/agents")
    suspend fun getAgents(): Response<List<AgentInfo>>

    companion object {
        fun create(retrofit: Retrofit = ApiClient.retrofit): AgentsApi = retrofit.create()
    }
}