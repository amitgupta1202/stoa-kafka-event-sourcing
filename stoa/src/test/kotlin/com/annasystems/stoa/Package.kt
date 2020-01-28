package com.annasystems.stoa

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import com.annasystems.stoa.common.*
import com.annasystems.stoa.common.Utils.createProducer
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.dao.SubmissionDao
import com.annasystems.stoa.submission.dao.SubmissionDao.Companion.SubmissionRecord
import com.annasystems.stoa.user.*
import com.annasystems.stoa.user.dao.UserDao
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.streams.StreamsConfig
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import java.time.Instant
import java.util.*

private val userProducer = createProducer(Configs.bootstrapServers, Configs.Topics.userCommands)
private val submissionTaskTriggerProducer = createProducer(Configs.bootstrapServers, Configs.Topics.submissionTaskTrigger)

internal val userDao = UserDao(UserDao.findUserInRedis { Configs.Redis.jedisPool.resource })
private val client: HttpHandler = OkHttp()

internal fun <T> Option<T>.get() = this.getOrElse { throw RuntimeException("value not defined") }
internal fun <L, R> Either<L, R>.get() = this.getOrElse { throw RuntimeException("value not defined") }

internal fun sendCommand(cmd: UserCommand) {
	userProducer.send(ProducerRecord(Configs.Topics.userCommands.name, cmd.metadata.userId, cmd)).get()
}

internal fun sendTaskOverdue(asOf: Instant, taskType: TaskType) {
	submissionTaskTriggerProducer.send(ProducerRecord(Configs.Topics.submissionTaskTrigger.name, asOf.toEpochMilli(), taskType.toString())).get()
}

internal fun fetchSubmission(submissionId: SubmissionId): Option<Submission> {
	val networkResponse: Response = client(Request(Method.GET, "http://localhost:9000/submissions/${submissionId.asString()}"))
	return networkResponse.bodyString().toSubmission().toOption()
}

internal fun fetchAllSubmissions(): List<SubmissionRecord> {
	val networkResponse: Response = client(Request(Method.GET, "http://localhost:9000/submissions"))
	return networkResponse.bodyString().toSubmissionRecords().get()
}

internal fun fetchSubmissionHistory(submissionId: SubmissionId): List<SubmissionDao.Companion.SubmissionHistory> {
	val networkResponse: Response = client(Request(Method.GET, "http://localhost:9000/submissions/${submissionId.asString()}/history"))
	return networkResponse.bodyString().toSubmissionHistories().get()
}

internal fun <T : SubmissionCommand>sendCommand(cmd: T): T {
	val json = Configs.Serialization.json.stringify(SubmissionCommand.serializer(), cmd)
	client(Request(Method.POST, "http://localhost:9000/submissions/async").body(json))
	return cmd
}

internal fun randomSubmissionId() = UUID.randomUUID().toSubmissionId()

internal fun checkKafkaConnection(): Either<ApplicationError, Unit> = doTry {
	val props = Properties()
	props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
	props["group.id"] = "check-kafka-connection"
	props["key.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
	props["value.deserializer"] = "org.apache.kafka.common.serialization.StringDeserializer"
	val simpleConsumer = KafkaConsumer<String, String>(props)
	simpleConsumer.listTopics()
}.map { Unit }

internal fun checkHttpServerIsRunning(): Either<ApplicationError, Unit> = doTry {
	fetchAllSubmissions()
}.map { Unit }