package com.annasystems.stoa.submission.serilaizers

import com.annasystems.stoa.submission.SubmissionId
import com.annasystems.stoa.submission.toSubmissionId
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

@Serializer(forClass = SubmissionId::class)
object SubmissionIdSerializer : KSerializer<SubmissionId> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("SubmissionIdDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: SubmissionId) {
		encoder.encodeString(obj.raw.toString())
	}

	override fun deserialize(decoder: Decoder): SubmissionId {
		return UUID.fromString(decoder.decodeString()).toSubmissionId()
	}
}