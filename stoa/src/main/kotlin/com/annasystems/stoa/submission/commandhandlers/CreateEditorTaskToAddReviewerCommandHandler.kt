package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.Invitation.Companion.State.PENDING
import com.annasystems.stoa.submission.CreateEditorTaskToAddReviewer
import com.annasystems.stoa.submission.TaskToAddReviewerCreated
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.user.GetEditor

class CreateEditorTaskToAddReviewerCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<CreateEditorTaskToAddReviewer, TaskToAddReviewerCreated, Submission> {

	override fun handle(cmd: CreateEditorTaskToAddReviewer, existing: Option<Submission>): Either<ApplicationError, CommandResult<TaskToAddReviewerCreated, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (editor) = getEditor(cmd.editorId)
		val noPendingTasks = !submission.editorInvitations.any { it.user == editor && it.state == PENDING }
		val (submissionEvent) = Either.cond(
			noPendingTasks,
			{ TaskToAddReviewerCreated(cmd.metadata.toEventMetadata(submission.version), cmd.editorId, cmd.overdue) },
			{ EditorAlreadyHasPendingTask(submission.id, cmd.editorId, cmd.overdue) }
		)
		CommandResult(submissionEvent, submissionEvent.apply(submission, editor))
	}
}