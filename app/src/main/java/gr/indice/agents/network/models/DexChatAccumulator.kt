package gr.indice.agents.network.models

import java.time.OffsetDateTime

data class DexChatAccumulator(
    var conversationId: String? = null,
    var createdAt: OffsetDateTime? = null,
    var finishReason: DexChatFinishReason? = null,
    var guestSession: GuestSession? = null,
    var limitReached: Boolean = false,
    var modelId: String? = null,
    var responseId: String? = null,
    val messages: MutableList<DexChatMessage> = mutableListOf(),
    val textBuilder: StringBuilder = StringBuilder(),
    var usage: DexChatUsage? = null,
    var currentTextTarget: TextTarget? = null
)
    data class TextTarget(
        val messageIndex: Int,
        val partIndex: Int
    )
