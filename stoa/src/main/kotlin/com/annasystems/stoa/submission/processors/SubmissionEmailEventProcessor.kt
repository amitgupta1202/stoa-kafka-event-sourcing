package com.annasystems.stoa.submission.processors

import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.submission.EmailEvent
import com.annasystems.stoa.submission.SubmissionEmail
import com.annasystems.stoa.submission.SubmissionEvent
import com.annasystems.stoa.submission.SubmissionId
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced

class SubmissionEmailEventProcessor(
	override val bootstrapServers: String,
	private val submissionEventTopic: Topic<SubmissionId, SubmissionEvent>,
	private val submissionEmailTopic: Topic<SubmissionId, SubmissionEmail>
) : StreamProcessorApp() {

	override val appId: String = "processor-submission-email-event"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val submissionEvents = with(submissionEventTopic) {
				builder.stream(name, Consumed.with(keySerde, valueSerde))
			}
			val emails = submissionEvents
				.filter { _, event -> event is EmailEvent }
				.mapValues { event -> (event as EmailEvent).email }

			with(submissionEmailTopic) { emails.to(name, Produced.with(keySerde, valueSerde)) }

			return builder.build()
		}
}
