package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.right
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.CommandHandler
import com.annasystems.stoa.common.CommandResult
import com.annasystems.stoa.common.SubmissionDoesNotExists
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.user.GetEditor

class SendEditorChaseToAddReviewerEmailCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<SendEditorChaseToAddReviewerEmail, EditorChaseToInviteReviewerEmailSent, Submission> {

	override fun handle(cmd: SendEditorChaseToAddReviewerEmail, existing: Option<Submission>): Either<ApplicationError, CommandResult<EditorChaseToInviteReviewerEmailSent, Submission>> =
		Either.fx {
			val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
			val (editor) = getEditor(cmd.editorId)
			val email = SubmissionEmail(
				emailAddress = editor.emailAddress,
				subject = "Chase to invite the reviewer",
				body = "Please invite some reviewer.",
				submissionId = cmd.metadata.submissionId,
				requestId = cmd.metadata.requestId,
				timestamp = cmd.metadata.timestamp,
				emailType = SubmissionEmailType.CHASE_EDITOR_TO_INVITE_REVIEWER
			)
			val (submissionEvent) = EditorChaseToInviteReviewerEmailSent(cmd.metadata.toEventMetadata(submission.version), cmd.editorId, email).right()
			CommandResult(submissionEvent, submissionEvent.apply(submission))
		}
}