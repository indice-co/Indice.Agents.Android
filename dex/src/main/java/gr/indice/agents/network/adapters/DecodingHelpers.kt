package gr.indice.agents.network.adapters

import gr.indice.agents.network.Serializer
import gr.indice.agents.network.models.ProblemDetails
import okhttp3.ResponseBody

internal object DecodingHelpers {

    fun ResponseBody.toProblemDetails() = runCatching {
        toType(ProblemDetails::class.java)
    }.getOrNull()

    fun <T> ResponseBody.toType(target: Class<T>) =
        Serializer.moshi.adapter(target)
            .run { fromJson(source().peek()) }

}