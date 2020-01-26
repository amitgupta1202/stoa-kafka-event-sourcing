package com.annasystems.stoa.common.serializers

import com.annasystems.stoa.common.Id
import com.annasystems.stoa.common.serdes.toUUID
import org.apache.kafka.common.serialization.Deserializer
import java.util.*

abstract class IdDeserializer<T : Id> : Deserializer<T> {
	override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
	override fun deserialize(topic: String?, data: ByteArray?): T {
		checkNotNull(data)
		return from(data.toUUID())
	}

	override fun close() {}

	abstract fun from(uuid: UUID): T
}