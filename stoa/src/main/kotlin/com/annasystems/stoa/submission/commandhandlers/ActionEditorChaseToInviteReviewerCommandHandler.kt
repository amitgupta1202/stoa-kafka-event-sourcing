package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.Chaser.Companion.State.SCHEDULED
import com.annasystems.stoa.user.GetEditor

class ActionEditorChaseToInviteReviewerCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<ActionEditorChaseToAddReviewer, EditorChaseToInviteReviewerActionReceived, Submission> {

	override fun handle(cmd: ActionEditorChaseToAddReviewer, existing: Option<Submission>): Either<ApplicationError, CommandResult<EditorChaseToInviteReviewerActionReceived, Submission>> =
		Either.fx {
			val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
			val (editor) = getEditor(cmd.editorId)
			val hasPendingScheduledChaser = submission.editorChasers.any { it.user.id == editor.id && it.state == SCHEDULED }
			val eventAction = when (cmd.action) {
				SubmissionCommand.Companion.ChaserAction.SEND -> SubmissionEvent.Companion.ChaserAction.SENT
				SubmissionCommand.Companion.ChaserAction.CANCEL -> SubmissionEvent.Companion.ChaserAction.CANCELLED
			}
			val (submissionEvent) = Either.cond(
				hasPendingScheduledChaser,
				{
					EditorChaseToInviteReviewerActionReceived(
						cmd.metadata.toEventMetadata(submission.version),
						cmd.editorId,
						cmd.chaseTime,
						ChaserType.EDITOR_TO_INVITE_REVIEWER,
						eventAction
					)
				},
				{ EditorHasNoScheduledChaser(submission.id, cmd.editorId, cmd.chaseTime) }
			)
			CommandResult(submissionEvent, submissionEvent.apply(submission, editor))
		}
}