package gr.indice.agents.network.models


import com.squareup.moshi.Json
import kotlinx.serialization.SerialName

enum class DexChatFinishReason {
     @Json(name = "stop")
     @SerialName("stop")
     Stop,

     @Json(name = "length")
     @SerialName("length")
     Length,

     @Json(name = "toolCalls")
     @SerialName("toolCalls")
     ToolCalls,

     @Json(name = "contentFilter")
     @SerialName("contentFilter")
     ContentFilter,

     @Json(name = "limit")
     @SerialName("limit")
     Limit
}
