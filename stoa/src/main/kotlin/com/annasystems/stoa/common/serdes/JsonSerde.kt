package com.annasystems.stoa.common.serdes

import arrow.core.getOrHandle
import arrow.core.identity
import com.annasystems.stoa.common.Configs.Serialization
import com.annasystems.stoa.common.doTry
import kotlinx.serialization.KSerializer
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.nio.charset.Charset

open class JsonConverters<T : Any>(private val kSerializer: KSerializer<T>) {
	fun toJson(data: T) = doTry(::identity) { Serialization.json.stringify(kSerializer, data) }

	fun toObject(jsonAsString: String) = doTry(::identity) { Serialization.json.parse(kSerializer, jsonAsString) }
}

class JsonSerde<T : Any>(private val kSerializer: KSerializer<T>) : JsonConverters<T>(kSerializer), Serde<T> {
	private val serializer = Serializer<T> { _, data ->
		checkNotNull(data)
		toJson(data).map { it.toByteArray(Charset.defaultCharset()) }.getOrHandle { throw it }
	}
	private val deserializer = Deserializer<T> { _, data ->
		checkNotNull(data)
		toObject(data.toString(Charset.defaultCharset())).getOrHandle { throw it }
	}

	override fun deserializer(): Deserializer<T> = deserializer
	override fun serializer(): Serializer<T> = serializer
}
