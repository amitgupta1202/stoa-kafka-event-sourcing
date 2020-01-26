package com.annasystems.stoa.common

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import com.annasystems.stoa.common.serdes.JsonSerde
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.processors.SubmissionCommandTransformer
import com.annasystems.stoa.submission.processors.SubmissionCommandTransformer.Companion.SubmissionCommandResult
import com.annasystems.stoa.submission.serdes.SubmissionIdSerde
import com.annasystems.stoa.user.User
import com.annasystems.stoa.user.UserCommand
import com.annasystems.stoa.user.UserEvent
import com.annasystems.stoa.user.UserId
import com.annasystems.stoa.user.processors.UserCommandTransformer
import com.annasystems.stoa.user.serdes.UserIdSerde
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

object Configs {
	val bootstrapServers: String = System.getenv("BOOTSTRAP_SERVERS") ?: "localhost:9092"

	object Serialization {
		val json: Json = Json(JsonConfiguration.Stable)
	}

	object Redis {
		val jedisPool: JedisPool = JedisPool(
			defaultRedisPoolConfig(),
			System.getenv("REDIS_HOST") ?: "localhost",
			doTry { System.getenv("REDIS_PORT").toInt() }.getOrElse { 6379 },
			doTry { System.getenv("REDIS_TIMEOUT").toInt() }.getOrElse { 2000 },
			System.getenv("REDIS_PASSWORD") ?: null)
	}

	object Postgres {
		val dataSource: DataSource = HikariDataSource(
			hikariConfig(
				System.getenv("POSTGRES_JDBC_URL") ?: "jdbc:postgresql://localhost:5432/stoa",
				System.getenv("POSTGRES_USERNAME") ?: "postgres",
				System.getenv("POSTGRES_PASSWORD") ?: "postgres"
			)
		)
	}

	object KeyValueStores {
		val submissions = "store-submissions"
		val users = "store-users"
	}

	@UseExperimental(ExperimentalTime::class)
	object Topics {
		val submissionEvents: Topic<SubmissionId, SubmissionEvent> = Topic(
			"topic-submission-events",
			SubmissionIdSerde,
			JsonSerde(SubmissionEvent.serializer()),
			retention = Some(Duration.INFINITE)
		)

		val submissionCommands: Topic<SubmissionId, SubmissionCommand> = Topic(
			"topic-submission-commands",
			SubmissionIdSerde,
			JsonSerde(SubmissionCommand.serializer()),
			retention = Some(30.minutes)
		)

		val submissionCommandResults: Topic<SubmissionId, SubmissionCommandResult> = Topic(
			"topic-submission-command-results",
			SubmissionIdSerde,
			JsonSerde(SubmissionCommandTransformer.Companion.SubmissionCommandResult.serializer()),
			retention = Some(30.minutes)
		)

		val submissionModels: Topic<SubmissionId, Submission> = Topic(
			"topic-submissions",
			SubmissionIdSerde,
			JsonSerde(Submission.serializer()),
			cleanupPolicy = Some(CleanupPolicy.Compact)
		)

		val submissionChaseTrigger: Topic<Long, String> = Topic(
			"topic-submission-chase-triggers",
			Serdes.Long(),
			Serdes.String(),
			cleanupPolicy = Some(CleanupPolicy.Compact)
		)

		val submissionEmails: Topic<SubmissionId, SubmissionEmail> = Topic(
			"topic-submission-emails",
			SubmissionIdSerde,
			JsonSerde(SubmissionEmail.serializer()),
			retention = Some(Duration.INFINITE)
		)

		val userEvents: Topic<UserId, UserEvent> = Topic(
			"topic-user-events",
			UserIdSerde,
			JsonSerde(UserEvent.serializer()),
			retention = Some(Duration.INFINITE)
		)

		val userCommands: Topic<UserId, UserCommand> = Topic(
			"topic-user-commands",
			UserIdSerde,
			JsonSerde(UserCommand.serializer()),
			retention = Some(30.minutes)
		)

		val userCommandResults: Topic<UserId, UserCommandTransformer.Companion.UserCommandResult> = Topic(
			"topic-user-command-results",
			UserIdSerde,
			JsonSerde(UserCommandTransformer.Companion.UserCommandResult.serializer()),
			retention = Some(30.minutes)
		)

		val userModels: Topic<UserId, User> = Topic(
			"topic-users",
			UserIdSerde,
			JsonSerde(User.serializer()),
			cleanupPolicy = Some(CleanupPolicy.Compact)
		)

		val all: Set<Topic<out Any, out Any>> =
			setOf(
				submissionEvents,
				submissionCommands,
				submissionCommandResults,
				submissionModels,
				submissionChaseTrigger,
				submissionEmails,
				userEvents,
				userCommands,
				userCommandResults,
				userModels
			)
	}
}


private fun hikariConfig(jdbcUrl: String, username: String, password: String): HikariConfig {
	val cfg = HikariConfig()
	cfg.jdbcUrl = jdbcUrl
	cfg.username = username
	cfg.password = password
	cfg.maximumPoolSize = 5
	cfg.minimumIdle = 2
	return cfg
}

private fun defaultRedisPoolConfig(): JedisPoolConfig {
	val poolConfig = JedisPoolConfig()
	with(poolConfig) {
		maxTotal = 12
		maxIdle = 12
		minIdle = 0
		testOnBorrow = true
		testOnReturn = true
		testWhileIdle = true
		minEvictableIdleTimeMillis = java.time.Duration.ofSeconds(60).toMillis()
		timeBetweenEvictionRunsMillis = java.time.Duration.ofSeconds(30).toMillis()
		numTestsPerEvictionRun = 3
		blockWhenExhausted = true
	}
	return poolConfig
}

sealed class CleanupPolicy {
	object Compact : CleanupPolicy()
}

@UseExperimental(ExperimentalTime::class)
data class Topic<K, V> constructor(
	val name: String,
	val keySerde: Serde<K>,
	val valueSerde: Serde<V>,
	val numPartitions: Int = 4,
	val replicationFactor: Short = 1,
	val retention: Option<Duration> = None,
	val cleanupPolicy: Option<CleanupPolicy> = None
) {

	val keySerializer: Serializer<K>
		get() = keySerde.serializer()

	val keyDeserializer: Deserializer<K>
		get() = keySerde.deserializer()

	val valueSerializer: Serializer<V> = valueSerde.serializer()

	val valueDeserializer: Deserializer<V> = valueSerde.deserializer()

	fun toNewTopic(): NewTopic {
		val emptyConfigs = emptyMap<String, String>()
		val withRetention = retention.foldLeft(emptyConfigs) { acc, value ->
			acc + (TopicConfig.RETENTION_MS_CONFIG to if (value.isInfinite()) "-1" else value.inMicroseconds.toLong().toString())
		}
		val withCleanupPolicy = cleanupPolicy.foldLeft(withRetention) { acc, value ->
			when (value) {
				CleanupPolicy.Compact -> acc + (TopicConfig.CLEANUP_POLICY_CONFIG to TopicConfig.CLEANUP_POLICY_COMPACT)
			}
		}
		return NewTopic(name, numPartitions, replicationFactor).configs(withCleanupPolicy)
	}
}

