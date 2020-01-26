package com.annasystems.stoa.common.serializers

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("UUIDDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: UUID) {
		encoder.encodeString(obj.toString())
	}

	override fun deserialize(decoder: Decoder): UUID {
		return UUID.fromString(decoder.decodeString())
	}
}