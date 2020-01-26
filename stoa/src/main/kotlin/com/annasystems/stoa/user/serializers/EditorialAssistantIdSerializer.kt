package com.annasystems.stoa.user.serializers

import com.annasystems.stoa.user.EditorialAssistantId
import com.annasystems.stoa.user.toEditorialAssistantId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = EditorialAssistantId::class)
object EditorialAssistantIdSerializer : KSerializer<EditorialAssistantId> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("EditorialAssistantIdDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: EditorialAssistantId) {
		encoder.encodeString(obj.raw.toString())
	}

	override fun deserialize(decoder: Decoder): EditorialAssistantId {
		return UUID.fromString(decoder.decodeString()).toEditorialAssistantId()
	}
}