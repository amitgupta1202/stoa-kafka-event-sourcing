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

class SendInviteEditorEmailCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<SendInviteEditorEmail, InviteEditorEmailSent, Submission> {

	override fun handle(cmd: SendInviteEditorEmail, existing: Option<Submission>): Either<ApplicationError, CommandResult<InviteEditorEmailSent, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (editor) = getEditor(cmd.editorId)
		val email = SubmissionEmail(
			emailAddress = editor.emailAddress,
			subject = "Invitation to be an editor for a submission",
			body = "Please click on link accept or decline.",
			submissionId = cmd.metadata.submissionId,
			requestId = cmd.metadata.requestId,
			timestamp = cmd.metadata.timestamp,
			emailType = SubmissionEmailType.INVITE_EDITOR
		)
		val (submissionEvent) = InviteEditorEmailSent(cmd.metadata.toEventMetadata(submission.version), cmd.editorId, email).right()
		CommandResult(submissionEvent, submissionEvent.apply(submission))
	}
}