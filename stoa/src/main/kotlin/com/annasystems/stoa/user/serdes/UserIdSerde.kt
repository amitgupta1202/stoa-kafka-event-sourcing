package com.annasystems.stoa.user.serdes

import com.annasystems.stoa.common.serializers.IdDeserializer
import com.annasystems.stoa.common.serializers.IdSerializer
import com.annasystems.stoa.user.DefaultUserId
import com.annasystems.stoa.user.UserId
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer
import java.util.*

object UserIdSerde : Serde<UserId> {
	private val serializer = object : IdSerializer<UserId>() {}
	private val deserializer = object : IdDeserializer<UserId>() {
		override fun from(uuid: UUID): UserId = DefaultUserId(uuid)
	}

	override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
	override fun serializer(): Serializer<UserId> = serializer
	override fun deserializer(): Deserializer<UserId> = deserializer
	override fun close() {}
}

