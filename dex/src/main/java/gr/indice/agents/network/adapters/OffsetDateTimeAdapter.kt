package gr.indice.agents.network.adapters
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.reflect.Type
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class OffsetDateTimeAdapter {
    @ToJson
    fun toJson(value: OffsetDateTime): String {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value)
    }

    @FromJson
    fun fromJson(value: String): OffsetDateTime {
        return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

}


class OffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): OffsetDateTime? {
        return OffsetDateTime.parse(json?.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}




object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        return OffsetDateTime.parse(decoder.decodeString())
    }
}