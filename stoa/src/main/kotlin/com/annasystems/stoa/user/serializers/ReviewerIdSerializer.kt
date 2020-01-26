package com.annasystems.stoa.user.serializers

import com.annasystems.stoa.user.ReviewerId
import com.annasystems.stoa.user.toReviewerId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = ReviewerId::class)
object ReviewerIdSerializer : KSerializer<ReviewerId> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("ReviewerIdDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: ReviewerId) {
		encoder.encodeString(obj.raw.toString())
	}

	override fun deserialize(decoder: Decoder): ReviewerId {
		return UUID.fromString(decoder.decodeString()).toReviewerId()
	}
}