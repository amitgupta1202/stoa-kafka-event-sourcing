package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.EditorInvitationResponseReceived
import com.annasystems.stoa.submission.Invitation.Companion.State.PENDING
import com.annasystems.stoa.submission.RespondToEditorInvitation
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.submission.SubmissionCommand.Companion.EditorInvitationResponse.ACCEPT
import com.annasystems.stoa.submission.SubmissionCommand.Companion.EditorInvitationResponse.DECLINE
import com.annasystems.stoa.submission.SubmissionEvent.Companion.EditorInvitationResponse.ACCEPTED
import com.annasystems.stoa.submission.SubmissionEvent.Companion.EditorInvitationResponse.DECLINED
import com.annasystems.stoa.user.GetEditor

class RespondToEditorInvitationCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<RespondToEditorInvitation, EditorInvitationResponseReceived, Submission> {

	override fun handle(cmd: RespondToEditorInvitation, existing: Option<Submission>): Either<ApplicationError, CommandResult<EditorInvitationResponseReceived, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (editor) = getEditor(cmd.editorId)
		val pendingInvitation = submission.editorInvitations.any { it.user.id == editor.id && it.state == PENDING }
		val noEditorAssigned = submission.editor.isEmpty()
		val eventResponse =
			when (cmd.response) {
				ACCEPT -> ACCEPTED
				DECLINE -> DECLINED
			}
		val (submissionEvent) = Either.cond(
			pendingInvitation && noEditorAssigned,
			{ EditorInvitationResponseReceived(cmd.metadata.toEventMetadata(submission.version), cmd.editorId, eventResponse) },
			{ EditorHasNoPendingInvitation(submission.id, cmd.editorId) }
		)
		CommandResult(submissionEvent, submissionEvent.apply(submission, editor))
	}
}