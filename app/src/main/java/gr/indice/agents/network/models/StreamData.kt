package gr.indice.agents.network.models

sealed interface StreamData {
    data class Started(val uuid: String): StreamData
    data class Status(val value: String): StreamData
    data object Changed: StreamData
    data object Ignored: StreamData
    data class Completed(val response: DexChatResponse): StreamData
}