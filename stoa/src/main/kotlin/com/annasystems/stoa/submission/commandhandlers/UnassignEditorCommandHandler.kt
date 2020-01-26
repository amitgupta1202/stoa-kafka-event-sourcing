package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.EditorUnassigned
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.submission.UnassignEditor
import com.annasystems.stoa.user.GetEditor

class UnassignEditorCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<UnassignEditor, EditorUnassigned, Submission> {

	override fun handle(cmd: UnassignEditor, existing: Option<Submission>): Either<ApplicationError, CommandResult<EditorUnassigned, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (editor) = getEditor(cmd.editorId)
		val (foundEditor) = submission.editor.toEither { EditorNotAssigned(submission.id, cmd.editorId) }
		val (submissionEvent) =
			Either.cond(
				foundEditor != editor,
				{ EditorUnassigned(cmd.metadata.toEventMetadata(submission.version), cmd.editorId) },
				{ EditorNotAssigned(submission.id, cmd.editorId) })
		CommandResult(submissionEvent, submissionEvent.apply(submission))
	}
}