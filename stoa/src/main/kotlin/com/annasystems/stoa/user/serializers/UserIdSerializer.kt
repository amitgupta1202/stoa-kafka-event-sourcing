package com.annasystems.stoa.user.serializers

import com.annasystems.stoa.user.UserId
import com.annasystems.stoa.user.toUserId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = UserId::class)
object UserIdSerializer : KSerializer<UserId> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("UserIdDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: UserId) {
		encoder.encodeString(obj.raw.toString())
	}

	override fun deserialize(decoder: Decoder): UserId {
		return UUID.fromString(decoder.decodeString()).toUserId()
	}
}