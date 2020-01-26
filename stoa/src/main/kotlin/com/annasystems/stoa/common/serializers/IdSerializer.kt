package com.annasystems.stoa.common.serializers

import com.annasystems.stoa.common.Id
import com.annasystems.stoa.common.serdes.toByteArray
import org.apache.kafka.common.serialization.Serializer

abstract class IdSerializer<T : Id> : Serializer<T> {
	override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
	override fun serialize(topic: String?, id: T?): ByteArray {
		checkNotNull(id)
		return id.raw.toByteArray()
	}

	override fun close() {}
}