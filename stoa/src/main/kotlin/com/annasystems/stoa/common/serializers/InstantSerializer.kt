package com.annasystems.stoa.common.serializers

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("InstantDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: Instant) {
		val timestamp = Timestamp.valueOf(LocalDateTime.ofInstant(obj, ZoneOffset.UTC))
		encoder.encodeString(timestamp.toString())
	}

	override fun deserialize(decoder: Decoder): Instant {
		return Timestamp.valueOf(decoder.decodeString()).toLocalDateTime().toInstant(ZoneOffset.UTC)
	}
}