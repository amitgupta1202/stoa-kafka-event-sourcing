package com.annasystems.stoa.submission.serilaizers

import com.annasystems.stoa.submission.Title
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = Title::class)
object TitleSerializer : KSerializer<Title> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("TitleDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: Title) {
		encoder.encodeString(obj.raw)
	}

	override fun deserialize(decoder: Decoder): Title {
		return Title(decoder.decodeString())
	}
}