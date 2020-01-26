package com.annasystems.stoa.submission.processors

import com.annasystems.stoa.common.Configs.Serialization
import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.SubmissionCommand.Companion.ChaserAction.SEND
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import redis.clients.jedis.Jedis

class SubmissionSendChaserProcessor(
	override val bootstrapServers: String,
	private val submissionChaseTriggerTopic: Topic<Long, String>,
	private val submissionCommandTopic: Topic<SubmissionId, SubmissionCommand>,
	private val jedis: () -> Jedis) : StreamProcessorApp() {

	override val appId: String = "processor-submission-send-chaser"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val chaseTriggerEvents =
				with(submissionChaseTriggerTopic) {
					builder.stream(name, Consumed.with(keySerde, valueSerde))
				}

			val submissionCommands: KStream<SubmissionId, SubmissionCommand> = chaseTriggerEvents.flatMap { chaseTime, chaserName ->
				jedis().use { it.zrangeByScore(chaserName, 0.0, chaseTime.toDouble()) }
					.map { Serialization.json.parse(SubmissionEvent.serializer(), it) }
					.mapNotNull { event ->
						when (event) {
							is ScheduledEditorChaseToInviteReviewer -> KeyValue.pair(
								event.metadata.submissionId,
								ActionEditorChaseToAddReviewer(event.metadata.toCommandMetadata(), event.editorId, event.chaseTime, SEND)
							)
							else -> null
						}
					}
			}

			with(submissionCommandTopic) {
				submissionCommands.to(name, Produced.with(keySerde, valueSerde))
			}

			return builder.build()
		}

	companion object {
		private fun SubmissionEvent.Companion.Metadata.toCommandMetadata(): SubmissionCommand.Companion.Metadata =
			with(this) { SubmissionCommand.Companion.Metadata(RequestId.generate(), actorId, timestamp, submissionId, expectedVersion = Version.ANY) }
	}
}
