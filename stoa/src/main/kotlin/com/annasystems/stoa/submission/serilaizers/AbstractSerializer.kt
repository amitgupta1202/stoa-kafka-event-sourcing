package com.annasystems.stoa.submission.serilaizers

import com.annasystems.stoa.submission.Abstract
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = Abstract::class)
object AbstractSerializer : KSerializer<Abstract> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("AbstractDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: Abstract) {
		encoder.encodeString(obj.raw)
	}

	override fun deserialize(decoder: Decoder): Abstract {
		return Abstract(decoder.decodeString())
	}
}