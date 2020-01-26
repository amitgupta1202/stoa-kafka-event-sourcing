package com.annasystems.stoa.submission.processors

import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.submission.SubmissionEvent
import com.annasystems.stoa.submission.SubmissionId
import com.annasystems.stoa.submission.dao.Rdbms.DESCRIPTION
import com.annasystems.stoa.submission.dao.Rdbms.EVENT_NUMBER
import com.annasystems.stoa.submission.dao.Rdbms.EVENT_TIME
import com.annasystems.stoa.submission.dao.Rdbms.EVENT_TYPE
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_HISTORY
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_ID
import com.annasystems.stoa.submission.mappers.ToSubmissionHistoryTypeAndDescription
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.jooq.impl.DSL
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class SubmissionHistoryProjectorProcessor(
	override val bootstrapServers: String,
	private val submissionEventTopic: Topic<SubmissionId, SubmissionEvent>,
	private val toSubmissionHistoryTypeAndDescription: ToSubmissionHistoryTypeAndDescription,
	private val connection: () -> Connection
) : StreamProcessorApp() {

	override val appId: String = "processor-submission-history-projector"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val submissionEvents =
				with(submissionEventTopic) {
					builder.stream(name, Consumed.with(keySerde, valueSerde))
				}

			submissionEvents
				.mapValues { event -> event.toHistoryRecord() }
				.foreach { _, record -> saveToDb(record) }

			return builder.build()
		}

	private fun saveToDb(record: HistoryRecord) {
		connection().use { connection ->
			with(record) {
				DSL.using(connection).mergeInto(SUBMISSION_HISTORY, SUBMISSION_ID, EVENT_NUMBER, EVENT_TIME, EVENT_TYPE, DESCRIPTION)
					.key(SUBMISSION_ID, EVENT_NUMBER)
					.values(submissionId, eventNumber, timestamp, eventType, eventDescription)
					.execute()
			}
		}
	}

	data class HistoryRecord(val submissionId: UUID, val eventNumber: Int, val timestamp: Timestamp, val eventType: String, val eventDescription: String)

	private fun SubmissionEvent.toHistoryRecord(): HistoryRecord {
		val eventTime = Timestamp.valueOf(LocalDateTime.ofInstant(metadata.timestamp, ZoneOffset.UTC))
		val (type, description) = toSubmissionHistoryTypeAndDescription.apply(this)
		return HistoryRecord(metadata.submissionId.raw, metadata.submissionVersion.num, eventTime, type, description)
	}
}