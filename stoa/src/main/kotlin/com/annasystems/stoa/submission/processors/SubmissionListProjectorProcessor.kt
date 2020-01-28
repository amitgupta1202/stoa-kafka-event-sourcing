package com.annasystems.stoa.submission.processors

import arrow.core.getOrElse
import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.submission.SubmissionId
import com.annasystems.stoa.submission.dao.Rdbms.ABSTRACT
import com.annasystems.stoa.submission.dao.Rdbms.CHASE_TIME
import com.annasystems.stoa.submission.dao.Rdbms.EDITOR_ID
import com.annasystems.stoa.submission.dao.Rdbms.REVIEWER_ID
import com.annasystems.stoa.submission.dao.Rdbms.STATE
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSIONS
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_EDITOR_TASKS
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_EDITOR_INVITATIONS
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_ID
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_REVIEWERS
import com.annasystems.stoa.submission.dao.Rdbms.TITLE
import com.annasystems.stoa.submission.dao.Rdbms.VERSION
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.sql.Connection
import java.sql.Connection.TRANSACTION_READ_COMMITTED
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class SubmissionListProjectorProcessor(override val bootstrapServers: String, private val submissionModelTopic: Topic<SubmissionId, Submission>, private val connection: () -> Connection) : StreamProcessorApp() {

	override val appId: String = "processor-submission-list-projector"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val submissions =
				with(submissionModelTopic) {
					builder.stream(name, Consumed.with(keySerde, valueSerde))
				}

			submissions
				.mapValues { submission -> submission.toSubmissionRecord() }
				.foreach { _, submissionRecord -> saveToDb(submissionRecord) }
			return builder.build()
		}

	private fun saveToDb(submission: SubmissionRecord) {
		connection().use { connection ->
			connection.autoCommit = false
			connection.transactionIsolation = TRANSACTION_READ_COMMITTED

			val dslContext = DSL.using(connection)

			try {
				writeSubmissionRecord(dslContext, submission)
				writeSubmissionReviewerRecords(dslContext, submission)
				writeSubmissionEditorInvitationRecords(dslContext, submission)
				writeSubmissionTaskRecords(dslContext, submission)
				connection.commit()
			} catch (t: Throwable) {
				connection.rollback()
			}
		}
	}

	private fun writeSubmissionTaskRecords(dslContext: DSLContext, submission: SubmissionRecord) {
		dslContext
			.deleteFrom(SUBMISSION_EDITOR_TASKS)
			.where(SUBMISSION_ID.eq(submission.submissionId))
			.execute()

		submission.editorTasks.forEach {
			dslContext
				.insertInto(SUBMISSION_EDITOR_TASKS, SUBMISSION_ID, EDITOR_ID, CHASE_TIME, STATE)
				.values(it.submissionId, it.editorId, it.chaseTime, it.state)
				.execute()
		}
	}

	private fun writeSubmissionEditorInvitationRecords(dslContext: DSLContext, submission: SubmissionRecord) {
		dslContext
			.deleteFrom(SUBMISSION_EDITOR_INVITATIONS)
			.where(SUBMISSION_ID.eq(submission.submissionId))
			.execute()

		submission.editorInvitations.forEach {
			dslContext
				.insertInto(SUBMISSION_EDITOR_INVITATIONS, SUBMISSION_ID, EDITOR_ID, STATE)
				.values(it.submissionId, it.editorId, it.state)
				.execute()
		}
	}

	private fun writeSubmissionReviewerRecords(dslContext: DSLContext, submission: SubmissionRecord) {
		dslContext
			.deleteFrom(SUBMISSION_REVIEWERS)
			.where(SUBMISSION_ID.eq(submission.submissionId))
			.execute()

		submission.reviewers.forEach {
			dslContext
				.insertInto(SUBMISSION_REVIEWERS, SUBMISSION_ID, REVIEWER_ID)
				.values(it.submissionId, it.reviewerId)
				.execute()
		}
	}

	private fun writeSubmissionRecord(dslContext: DSLContext, submission: SubmissionRecord) {
		dslContext
			.mergeInto(SUBMISSIONS, SUBMISSION_ID, TITLE, ABSTRACT, EDITOR_ID, VERSION)
			.key(SUBMISSION_ID)
			.values(submission.submissionId, submission.title, submission.abstract, submission.editor, submission.version)
			.execute()
	}

	private fun Submission.toSubmissionRecord(): SubmissionRecord {
		val editor = editor.map { it.id.raw }.getOrElse { null }
		val reviewerRecords = reviewers.map { SubmissionReviewerRecord(id.raw, it.id.raw) }
		val editorInvitationRecords = editorInvitations.map { SubmissionEditorInvitationRecord(id.raw, it.user.id.raw, it.state.toString()) }
		val editorTaskRecords = editorTasks.map {
			val overdue = Timestamp.valueOf(LocalDateTime.ofInstant(it.overdue, ZoneOffset.UTC))
			SubmissionEditorTaskRecord(id.raw,  it.user.id.raw, overdue, it.state.toString())
		}
		return SubmissionRecord(id.raw, title.raw, abstract.raw, version.num, editor, reviewerRecords, editorInvitationRecords, editorTaskRecords)
	}

	data class SubmissionRecord(
		val submissionId: UUID,
		val title: String,
		val abstract: String,
		val version: Int,
		val editor: UUID?,
		val reviewers: List<SubmissionReviewerRecord>,
		val editorInvitations: List<SubmissionEditorInvitationRecord>,
		val editorTasks: List<SubmissionEditorTaskRecord>

	)
	data class SubmissionReviewerRecord(val submissionId: UUID, val reviewerId: UUID)
	data class SubmissionEditorInvitationRecord(val submissionId: UUID, val editorId: UUID, val state: String)
	data class SubmissionEditorTaskRecord(val submissionId: UUID, val editorId: UUID, val chaseTime: Timestamp, val state: String)
}