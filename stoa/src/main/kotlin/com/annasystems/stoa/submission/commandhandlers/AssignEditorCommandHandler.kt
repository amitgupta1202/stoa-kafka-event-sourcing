package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.AssignEditor
import com.annasystems.stoa.submission.EditorAssigned
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.user.GetEditor

class AssignEditorCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<AssignEditor, EditorAssigned, Submission> {

	override fun handle(cmd: AssignEditor, existing: Option<Submission>): Either<ApplicationError, CommandResult<EditorAssigned, Submission>> =
		Either.fx {
			val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
			val (editor) = getEditor(cmd.editorId)
			val (submissionEvent) = submission.editor
				.toEither { EditorAssigned(cmd.metadata.toEventMetadata(submission.version), cmd.editorId) }
				.map { EditorAlreadyAssigned(submission.id, cmd.editorId) }
				.swap()
			CommandResult(submissionEvent, submissionEvent.apply(submission, editor))
		}
}