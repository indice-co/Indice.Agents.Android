package gr.indice.agents.network.services

import gr.indice.agents.dex.BuildConfig
import kotlinx.coroutines.CancellationException


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

internal suspend fun <T> safeCall(block: suspend () -> T): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) e.printStackTrace()
        null
    }
