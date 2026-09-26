package com.thebackendguy.myandroidtestapp.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/*
 * MongoDB sends money either as a plain number, a numeric string, or a
 * Decimal128 object ({"$numberDecimal": "10"}). These read all three, and
 * fall back to 0 / "" instead of failing the whole list over one odd value.
 */

private fun JsonElement.asNumberText(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> this["\$numberDecimal"]?.jsonPrimitive?.contentOrNull
    else -> null
}

object LenientDouble : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)
    override fun deserialize(decoder: Decoder): Double =
        (decoder as JsonDecoder).decodeJsonElement().asNumberText()?.toDoubleOrNull() ?: 0.0
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}

object LenientInt : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Int =
        (decoder as JsonDecoder).decodeJsonElement().asNumberText()?.toDoubleOrNull()?.toInt() ?: 0
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

/** Any number or string as text, such as a pincode stored as a number. */
object LenientText : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("LenientText", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): String =
        (decoder as JsonDecoder).decodeJsonElement().asNumberText()?.removeSuffix(".0").orEmpty()
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}
