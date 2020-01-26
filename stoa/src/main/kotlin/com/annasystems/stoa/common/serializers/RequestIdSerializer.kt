package com.annasystems.stoa.common.serializers

import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.toRequestId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = RequestId::class)
object RequestIdSerializer : KSerializer<RequestId> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("RequestIdDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: RequestId) {
		encoder.encodeString(obj.raw.toString())
	}

	override fun deserialize(decoder: Decoder): RequestId {
		return UUID.fromString(decoder.decodeString()).toRequestId()
	}
}