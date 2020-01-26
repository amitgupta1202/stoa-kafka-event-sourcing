package com.annasystems.stoa.user.processors

import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.user.User
import com.annasystems.stoa.user.UserId
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import redis.clients.jedis.Jedis

class UserProjectorProcessor(
	override val bootstrapServers: String,
	private val userModelTopic: Topic<UserId, User>,
	private val json: kotlinx.serialization.json.Json,
	private val jedis: () -> Jedis
) : StreamProcessorApp() {

	override val appId: String = "processor-user-projector"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val submissions =
				with(userModelTopic) {
					builder.stream(name, Consumed.with(keySerde, valueSerde))
				}

			submissions.foreach { userId, user ->
				jedis().use { it.set(userId.asString(), json.stringify(User.serializer(), user)) }
			}

			return builder.build()
		}
}