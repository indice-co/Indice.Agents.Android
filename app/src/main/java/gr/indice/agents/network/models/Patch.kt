package gr.indice.agents.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Patch(val path: String? = null, val op: String? = null, val value: JsonElement? = null)