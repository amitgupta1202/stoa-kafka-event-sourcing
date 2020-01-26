package com.annasystems.stoa.common.serdes

import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.serializers.IdDeserializer
import com.annasystems.stoa.common.serializers.IdSerializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.util.*

object RequestIdSerde : Serde<RequestId> {
	private val serializer = object : IdSerializer<RequestId>() {}
	private val deserializer = object : IdDeserializer<RequestId>() {
		override fun from(uuid: UUID): RequestId = RequestId(uuid)
	}

	override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
	override fun serializer(): Serializer<RequestId> = serializer
	override fun deserializer(): Deserializer<RequestId> = deserializer
	override fun close() {}
}