package gr.indice.agents.network



internal interface Service {
    val sseService: SseService
}

internal object ServiceInit {
    fun create(): Service = ServiceImpl()
}

internal class ServiceImpl: Service {
    override val sseService = SseServiceImpl()

}

