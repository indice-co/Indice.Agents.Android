package gr.indice.agents.dex.models

import java.net.URI

sealed interface ChatContentType {
    data class Text(val value: String): ChatContentType
    data class Markdown(val value: List<Block>): ChatContentType {
        sealed interface Block {
            data class Text(val text: String) : Block
            data class Code(val code: String) : Block
        }
    }
    data class Html(val value: String): ChatContentType
    class ImageData(val data: ByteArray, val mediaType: String): ChatContentType {
        override fun equals(other: Any?) =
            other is ImageData && mediaType == other.mediaType && data.contentEquals(other.data)

        override fun hashCode() = 31 * data.contentHashCode() + mediaType.hashCode()
    }
    data class ImageUrl(val url: URI): ChatContentType
    data class MultipleChoice(val data: List<String>): ChatContentType
    data class Callout(val value: gr.indice.agents.dex.models.Callout): ChatContentType
    data class Confirmation(val data: ConfirmData): ChatContentType
    data class Unsupported(val mediaType: String?): ChatContentType
    data class Unavailable(val mediaType: String?): ChatContentType
}

data class Callout(
    val severity: Severity,
    val title: String?,
    val text: String
)

data class ConfirmData(
    val prompt: String?,
    val confirmText: String,
    val cancelText: String
)