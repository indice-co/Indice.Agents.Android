package gr.indice.agents.network.models

import kotlinx.serialization.Serializable

@Serializable
data class DexChatUsage(
    val inputTokenCount: Int? = null,
    val outputTokenCount: Int? = null,
    val questionsLimitCount: Int? = null,
    val questionsUsedCount: Int? = null,
    val totalTokenCount: Int? = null
)