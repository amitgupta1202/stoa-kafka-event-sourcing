package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.Invitation.Companion.State.PENDING
import com.annasystems.stoa.submission.ScheduleEditorChaseToAddReviewer
import com.annasystems.stoa.submission.ScheduledEditorChaseToInviteReviewer
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.user.GetEditor

class ScheduleEditorChaseToAddReviewerCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<ScheduleEditorChaseToAddReviewer, ScheduledEditorChaseToInviteReviewer, Submission> {

	override fun handle(cmd: ScheduleEditorChaseToAddReviewer, existing: Option<Submission>): Either<ApplicationError, CommandResult<ScheduledEditorChaseToInviteReviewer, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (editor) = getEditor(cmd.editorId)
		val noScheduledChaser = !submission.editorInvitations.any { it.user == editor && it.state == PENDING }
		val (submissionEvent) = Either.cond(
			noScheduledChaser,
			{ ScheduledEditorChaseToInviteReviewer(cmd.metadata.toEventMetadata(submission.version), cmd.editorId, cmd.chaseTime) },
			{ EditorAlreadyHasScheduledChaser(submission.id, cmd.editorId, cmd.chaseTime) }
		)
		CommandResult(submissionEvent, submissionEvent.apply(submission, editor))
	}
}