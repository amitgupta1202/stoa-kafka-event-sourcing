package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.EditorInvited
import com.annasystems.stoa.submission.InviteEditor
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.user.GetEditor

class InviteEditorCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<InviteEditor, EditorInvited, Submission> {

	override fun handle(cmd: InviteEditor, existing: Option<Submission>): Either<ApplicationError, CommandResult<EditorInvited, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (editor) = getEditor(cmd.editorId)
		val notInvitedYet = !submission.editorInvitations.any { it.user == editor }

		val (submissionEvent) = Either.cond(
			notInvitedYet,
			{ EditorInvited(cmd.metadata.toEventMetadata(submission.version), cmd.editorId) },
			{ EditorAlreadyInvited(submission.id, cmd.editorId) }
		)
		CommandResult(submissionEvent, submissionEvent.apply(submission, editor))
	}
}