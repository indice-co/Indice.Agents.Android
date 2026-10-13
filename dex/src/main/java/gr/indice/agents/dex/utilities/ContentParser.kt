package gr.indice.agents.dex.utilities

import gr.indice.agents.dex.models.Callout
import gr.indice.agents.dex.models.ChatContent
import gr.indice.agents.dex.models.ChatContentType
import gr.indice.agents.dex.models.ConfirmData
import gr.indice.agents.dex.models.ImageReference
import gr.indice.agents.dex.models.Severity
import gr.indice.agents.network.models.ChatMessageContent
import gr.indice.agents.network.models.ChatMessagePart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Base64

fun String?.mediaType(): String? {
   return this?.substringBefore(';').nonempty()?.lowercase()
}
fun String?.nonempty(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }

fun JsonElement?.string(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    if (!primitive.isString) return null
    return primitive.content.takeIf { it.isNotBlank() }
}

private fun jsonObject(value: String): JsonObject? =
    runCatching { Json.parseToJsonElement(value) as? JsonObject }.getOrNull()


object ChatContentMapper {
    private const val MULTIPLE_CHOICE = "application/vnd.indice.multiple-choice+json"
    private const val CALLOUT = "application/vnd.indice.callout+json"
    private const val CONFIRM = "application/vnd.indice.confirm+json"
    private const val IMAGE_ENVELOPE = "application/vnd.indice.image+json"

    private val TEXT_TYPES = setOf("text/html", "text/plain", "text/markdown", "text")
    private val json = Json { ignoreUnknownKeys = true }

    private data class Classified(val content: ChatContentType, val caption: String?)

    fun items(content: ChatMessageContent?): List<ChatContent> =
        items(content, emptyList(), emptyList())

    internal fun items(
        content: ChatMessageContent?,
        previousParts: List<ChatMessagePart>,
        previousItems: List<ChatContent>,
    ): List<ChatContent> = (content?.parts ?: emptyList()).mapIndexed { index, part ->
        if (index in previousParts.indices &&
            index in previousItems.indices &&
            previousParts[index] == part
        ) {
            previousItems[index]
        } else {
            val (parsed, caption) = classify(part)
            ChatContent(id = index, content = parsed, caption = caption)
        }
    }

    private fun classify(part: ChatMessagePart): Classified {
        val type = part.contentType.mediaType()
        val caption = part.name.nonempty()
        val value = part.value

        envelopeContent(type, value)?.let { return Classified(it, caption) }

        if (type == IMAGE_ENVELOPE) {
            val image = runCatching { json.decodeFromString<ImageReference>(value) }.getOrNull()
            val uri = image?.let { it.uri ?: it.url }
                ?: return Classified(ChatContentType.Unavailable(type), caption)
            return Classified(
                imageContent(uri, declaredType = null),
                image.caption.nonempty() ?: image.alt.nonempty() ?: caption,
            )
        }

        val trimmed = value.trim()
        val content = when {
            type?.startsWith("image/") == true -> imageContent(trimmed, type)

            trimmed.startsWith("data:", ignoreCase = true) &&
                    (type == null || type == "text" || type.startsWith("text/")) -> dataUriContent(trimmed, type)

            else -> textContent(type, value)
        }
        return Classified(content, caption)

    }

    private fun textContent(type: String?, value: String): ChatContentType = when (type) {
        null, "text/plain" -> ChatContentType.Text(value)
        "text/markdown", "text" -> {
            ChatContentType.Markdown(parseBlocks(value))
        }
        "text/html" -> ChatContentType.Html(value)
        else -> ChatContentType.Unsupported(type)
    }

    private fun parseBlocks(input: String): List<ChatContentType.Markdown.Block> =
        input.split("```").mapIndexedNotNull { index, part ->
            if (index % 2 == 0) {
                part.trim().takeIf { it.isNotEmpty() }?.let { ChatContentType.Markdown.Block.Text(it) }
            } else {
                val code = part.removePrefix("\n")
                ChatContentType.Markdown.Block.Code(
                    code = code.trimEnd()
                )
            }
        }

    private fun dataUriContent(trimmed: String, type: String?): ChatContentType {
        val uri = DataUri.parse(trimmed) ?: return ChatContentType.Unavailable(type)
        return when {
            uri.mediaType.startsWith("image/") -> ChatContentType.ImageData(uri.data, uri.mediaType)
            uri.mediaType !in TEXT_TYPES -> ChatContentType.Unsupported(uri.mediaType)
            else -> uri.text?.let { textContent(uri.mediaType, it) } ?: ChatContentType.Unavailable(uri.mediaType)
        }
    }

    private fun imageContent(raw: String, declaredType: String?): ChatContentType {
        val value = raw.trim()
        if (value.startsWith("data:", ignoreCase = true)) {
            return DataUri.parse(value)
                ?.takeIf { it.mediaType.startsWith("image/") }
                ?.let { ChatContentType.ImageData(it.data, it.mediaType) }
                ?: ChatContentType.Unavailable(declaredType)
        }
        val url = runCatching { URI(value) }.getOrNull()
        val isSafe = url != null &&
                url.scheme?.lowercase() in setOf("http", "https") &&
                !url.host.isNullOrEmpty() &&
                url.userInfo == null
        return if (isSafe) ChatContentType.ImageUrl(url) else ChatContentType.Unavailable(declaredType)
    }

    private fun envelopeContent(type: String?, value: String): ChatContentType? = when (type) {
        MULTIPLE_CHOICE -> jsonObject(value)
            ?.let { fields ->
                val options = (fields["options"] as? JsonArray)
                    ?.mapNotNull { it.string() }
                    .orEmpty()
                ChatContentType.MultipleChoice(options)
            }
            ?: ChatContentType.Unavailable(type)
        CALLOUT -> jsonObject(value)
            ?.let { fields ->
                fields["text"].string()?.let { text ->
                    val severity = fields["severity"].string() ?: "info"
                    ChatContentType.Callout(
                        Callout(
                            severity = Severity.entries.find { it.name.equals(severity, ignoreCase = true) } ?: Severity.Info,
                            title = fields["title"].string(),
                            text = text,
                        )
                    )
                }
            }
            ?: ChatContentType.Unavailable(type)
        CONFIRM -> jsonObject(value)
            ?.let { fields ->
                ChatContentType.Confirmation(
                    ConfirmData(
                        prompt = fields["prompt"].string(),
                        confirmText = fields["confirmText"].string() ?: "Yes",
                        cancelText = fields["cancelText"].string() ?: "No",
                    )
                )
            }
            ?: ChatContentType.Unavailable(type)
        else -> null
    }
}


private class DataUri private constructor(
    val mediaType: String,
    val data: ByteArray,
    private val charset: String?,
) {
    /** Strictly decoded text, or null for an unknown charset or malformed bytes. */
    val text: String?
        get() {
            val cs = if (charset == null) Charsets.UTF_8 else CHARSETS[charset] ?: return null
            return try {
                cs.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data))
                    .toString()
            } catch (_: CharacterCodingException) {
                null
            }
        }

    companion object {
        private const val MAXIMUM_BYTES = 8 * 1024 * 1024
        private val TRANSPORT_WHITESPACE = setOf<Byte>(9, 10, 13, 32)

        private val CHARSETS: Map<String, Charset> = mapOf(
            "utf-8" to Charsets.UTF_8,
            "utf8" to Charsets.UTF_8,
            "us-ascii" to Charsets.US_ASCII,
            "ascii" to Charsets.US_ASCII,
            "iso-8859-1" to Charsets.ISO_8859_1,
            "latin1" to Charsets.ISO_8859_1,
            "windows-1252" to Charset.forName("windows-1252"),
        )

        fun parse(raw: String): DataUri? {
            if (!raw.startsWith("data:", ignoreCase = true)) return null
            val comma = raw.indexOf(',').takeIf { it >= 0 } ?: return null

            val header = raw.substring(5, comma).split(';')
            val mediaType = header.first().trim().lowercase().ifEmpty { "text/plain" }
            val parameters = header.drop(1).map { it.trim().lowercase() }
            val charset = parameters
                .firstOrNull { it.startsWith("charset=") }
                ?.removePrefix("charset=")
                ?.trim('"')

            // Bound allocation before percent/base64 decoding as well as afterwards.
            val payload = raw.substring(comma + 1).toByteArray(Charsets.UTF_8)
            if (payload.size > MAXIMUM_BYTES * 4) return null
            val bytes = percentDecode(payload) ?: return null

            val data = if ("base64" in parameters) {
                // Only transport whitespace is removable; '+' stays '+'.
                val stripped = bytes.filterNot { it in TRANSPORT_WHITESPACE }.toByteArray()
                if (stripped.size % 4 != 0) return null // reject missing padding
                runCatching { Base64.getDecoder().decode(stripped) }.getOrNull() ?: return null
            } else {
                bytes
            }
            return if (data.size <= MAXIMUM_BYTES) DataUri(mediaType, data, charset) else null
        }

        // %HH is an encoded byte, not necessarily a UTF-8 character.
        private fun percentDecode(input: ByteArray): ByteArray? {
            val out = ByteArrayOutputStream(input.size)
            var i = 0
            while (i < input.size) {
                val byte = input[i].toInt() and 0xFF
                if (byte == '%'.code) {
                    val high = input.getOrNull(i + 1)?.let(::hexDigit) ?: return null
                    val low = input.getOrNull(i + 2)?.let(::hexDigit) ?: return null
                    out.write(high * 16 + low)
                    i += 3
                } else {
                    out.write(byte)
                    i++
                }
            }
            return out.toByteArray()
        }

        private fun hexDigit(byte: Byte): Int? =
            Character.digit(byte.toInt() and 0xFF, 16).takeIf { it >= 0 }
    }
}