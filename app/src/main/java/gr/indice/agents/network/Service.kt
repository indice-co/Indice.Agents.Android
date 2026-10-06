package gr.indice.agents.network



interface Service {
    val sseService: SseService
}

object ServiceInit {
    fun create(): Service = ServiceImpl()
}

internal class ServiceImpl: Service {
    override val sseService = SseServiceImpl()

}

