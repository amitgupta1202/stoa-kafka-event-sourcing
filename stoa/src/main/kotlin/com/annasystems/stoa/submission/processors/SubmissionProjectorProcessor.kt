package com.annasystems.stoa.submission.processors

import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.submission.SubmissionId
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import redis.clients.jedis.Jedis

class SubmissionProjectorProcessor(
	override val bootstrapServers: String,
	private val submissionModels: Topic<SubmissionId, Submission>,
	private val json: kotlinx.serialization.json.Json,
	private val jedis: () -> Jedis
) : StreamProcessorApp() {

	override val appId: String = "processor-submission-projector"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val submissions =
				with(submissionModels) { builder.stream(name, Consumed.with(keySerde, valueSerde)) }

			submissions.foreach { submissionId, submission ->
				jedis().use { it.set(submissionId.asString(), json.stringify(Submission.serializer(), submission)) }
			}

			return builder.build()
		}
}