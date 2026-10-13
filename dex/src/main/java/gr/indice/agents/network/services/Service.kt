package gr.indice.agents.network.services


internal interface Service {
    val sseService: SseService
    val agentsService: AgentsService
}

internal object ServiceInit {
    fun create(): Service = ServiceImpl()
}

internal class ServiceImpl: Service {
    override val sseService = SseServiceImpl()
    override val agentsService = AgentsServiceImpl()

}

