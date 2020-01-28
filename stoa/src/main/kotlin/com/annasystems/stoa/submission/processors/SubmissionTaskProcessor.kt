package com.annasystems.stoa.submission.processors

import com.annasystems.stoa.common.Configs.Serialization
import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.submission.ScheduleChaseEvent
import com.annasystems.stoa.submission.SubmissionEvent
import com.annasystems.stoa.submission.SubmissionId
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import redis.clients.jedis.Jedis

class SubmissionTaskProcessor(
	override val bootstrapServers: String,
	private val submissionEventTopic: Topic<SubmissionId, SubmissionEvent>,
	private val jedis: () -> Jedis
) : StreamProcessorApp() {

	override val appId: String = "processor-submission-task"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val submissionEvents =
				with(submissionEventTopic) {
					builder.stream(name, Consumed.with(keySerde, valueSerde))
				}

			submissionEvents.foreach { _, event ->
				if (event is ScheduleChaseEvent) {
					val json = Serialization.json.stringify(SubmissionEvent.serializer(), event)
					val score = event.chaseTime.toEpochMilli().toDouble()
					jedis().use { it.zadd(event.taskType.toString(), score, json) }
				}
			}
			return builder.build()
		}
}