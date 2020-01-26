package com.annasystems.stoa.user.serializers

import com.annasystems.stoa.user.EditorId
import com.annasystems.stoa.user.toEditorId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = EditorId::class)
object EditorIdSerializer : KSerializer<EditorId> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("EditorIdDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: EditorId) {
		encoder.encodeString(obj.raw.toString())
	}

	override fun deserialize(decoder: Decoder): EditorId {
		return UUID.fromString(decoder.decodeString()).toEditorId()
	}
}