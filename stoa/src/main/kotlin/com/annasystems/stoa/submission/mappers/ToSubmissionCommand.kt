package com.annasystems.stoa.submission.mappers

import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.none
import arrow.core.some
import com.annasystems.stoa.common.Mapper
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.SubmissionCommand.Companion.TaskState.DONE
import com.annasystems.stoa.submission.SubmissionEvent.Companion.TaskState.OVERDUE
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
				CreateEditorTaskToAddReviewer(from.metadata.toCommandMetadata(submission.version), from.editorId, chaseTime).some()
			}

			is ReviewerAdded -> Option.fx {
				val (editor) = submission.editor
				val (task) = Option.fromNullable(submission.editorTasks.find { task -> task.user.id == editor.id })
				ChangeEditorTaskToAddReviewer(from.metadata.toCommandMetadata(submission.version), editor.id, task.overdue, DONE)
			}

			is TaskToAddReviewerChanged ->
				if (from.state == OVERDUE) SendEditorChaseToAddReviewerEmail(from.metadata.toCommandMetadata(submission.version), from.editorId).some() else none()

			else -> none()
		}
}
