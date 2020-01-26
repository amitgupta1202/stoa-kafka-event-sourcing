package com.annasystems.stoa.submission.serdes

import com.annasystems.stoa.common.serializers.IdDeserializer
import com.annasystems.stoa.common.serializers.IdSerializer
import com.annasystems.stoa.submission.SubmissionId
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.util.*

object SubmissionIdSerde : Serde<SubmissionId> {
	private val serializer = object : IdSerializer<SubmissionId>() {}
	private val deserializer = object : IdDeserializer<SubmissionId>() {
		override fun from(uuid: UUID): SubmissionId = SubmissionId(uuid)
	}

	override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
	override fun serializer(): Serializer<SubmissionId> = serializer
	override fun deserializer(): Deserializer<SubmissionId> = deserializer
	override fun close() {}
}