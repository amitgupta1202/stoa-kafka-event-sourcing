package com.annasystems.stoa.submission.mappers

import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.none
import arrow.core.some
import com.annasystems.stoa.common.Mapper
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.SubmissionCommand.Companion.ChaserAction.CANCEL
import com.annasystems.stoa.submission.SubmissionEvent.Companion.ChaserAction.SENT
import com.annasystems.stoa.submission.SubmissionEvent.Companion.EditorInvitationResponse.ACCEPTED
import java.time.temporal.ChronoUnit

class ToSubmissionCommand(private val submission: Submission) : Mapper<SubmissionEvent, Option<SubmissionCommand>> {
	override fun apply(from: SubmissionEvent): Option<SubmissionCommand> =
		when (from) {
			is EditorInvited ->
				SendInviteEditorEmail(
					metadata = from.metadata.toCommandMetadata(submission.version),
					editorId = from.editorId
				).some()

			is EditorInvitationResponseReceived -> {
				if (from.response == ACCEPTED) AssignEditor(from.metadata.toCommandMetadata(submission.version), from.editorId).some() else none()
			}

			is EditorAssigned -> {
				val chaseTime = from.metadata.timestamp.plus(3, ChronoUnit.DAYS)
				ScheduleEditorChaseToAddReviewer(from.metadata.toCommandMetadata(submission.version), from.editorId, chaseTime).some()
			}

			is ReviewerAdded -> Option.fx {
				val (editor) = submission.editor
				val (chaser) = Option.fromNullable(submission.editorChasers.find { chaser -> chaser.user.id == editor.id })
				ActionEditorChaseToAddReviewer(from.metadata.toCommandMetadata(submission.version), editor.id, chaser.chaseTime, CANCEL)
			}

			is EditorChaseToInviteReviewerActionReceived ->
				if (from.action == SENT) SendEditorChaseToAddReviewerEmail(from.metadata.toCommandMetadata(submission.version), from.editorId).some() else none()

			else -> none()
		}
}
