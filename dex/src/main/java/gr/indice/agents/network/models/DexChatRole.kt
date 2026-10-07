package gr.indice.agents.network.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName

@JsonClass(generateAdapter = false)
enum class DexChatRole {
    @SerialName("user")
    @Json(name = "user")
    User,

    @SerialName("assistant")
    @Json(name = "assistant")
    Assistant,

    @SerialName("system")
    @Json(name = "system")
    System,

    @SerialName("tool")
    @Json(name = "tool")
    Tool
}