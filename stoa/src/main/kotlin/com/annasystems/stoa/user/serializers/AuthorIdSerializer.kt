package com.annasystems.stoa.user.serializers

import com.annasystems.stoa.user.AuthorId
import com.annasystems.stoa.user.toAuthorId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = AuthorId::class)
object AuthorIdSerializer : KSerializer<AuthorId> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("AuthorIdDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: AuthorId) {
		encoder.encodeString(obj.raw.toString())
	}

	override fun deserialize(decoder: Decoder): AuthorId {
		return UUID.fromString(decoder.decodeString()).toAuthorId()
	}
}