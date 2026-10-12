package gr.indice.agents.network.services

import gr.indice.agents.network.api.AgentsApi
import gr.indice.agents.network.models.AgentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AgentsService {

    val agents: StateFlow<List<AgentInfo>>

    suspend fun getAgents()
}

internal class AgentsServiceImpl(
    private val api: AgentsApi = AgentsApi.create()
): AgentsService {

    private val _agents = MutableStateFlow<List<AgentInfo>>(emptyList())
    override val agents = _agents.asStateFlow()

    override suspend fun getAgents() {
        api.getAgents().takeIf { it.isSuccessful }?.let {
            _agents.value = it.body().orEmpty()
        }
    }

}