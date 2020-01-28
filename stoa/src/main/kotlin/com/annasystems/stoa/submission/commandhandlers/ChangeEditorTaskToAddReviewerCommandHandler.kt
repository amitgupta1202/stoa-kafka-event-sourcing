package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.Task.Companion.State.PENDING
import com.annasystems.stoa.user.GetEditor

class ChangeEditorTaskToAddReviewerCommandHandler(
	private val getEditor: GetEditor
) : CommandHandler<ChangeEditorTaskToAddReviewer, TaskToAddReviewerChanged, Submission> {

	override fun handle(cmd: ChangeEditorTaskToAddReviewer, existing: Option<Submission>): Either<ApplicationError, CommandResult<TaskToAddReviewerChanged, Submission>> =
		Either.fx {
			val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
			val (editor) = getEditor(cmd.editorId)
			val hasPendingTasks = submission.editorTasks.any { it.user.id == editor.id && it.state == PENDING }
			val eventAction = when (cmd.state) {
				SubmissionCommand.Companion.TaskState.OVERDUE -> SubmissionEvent.Companion.TaskState.OVERDUE
				SubmissionCommand.Companion.TaskState.DONE -> SubmissionEvent.Companion.TaskState.DONE
			}
			val (submissionEvent) = Either.cond(
				hasPendingTasks,
				{
					TaskToAddReviewerChanged(
						cmd.metadata.toEventMetadata(submission.version),
						cmd.editorId,
						cmd.overdue,
						TaskType.EDITOR_TO_INVITE_REVIEWER,
						eventAction
					)
				},
				{ EditorHasNoPendingTask(submission.id, cmd.editorId, cmd.overdue) }
			)
			CommandResult(submissionEvent, submissionEvent.apply(submission, editor))
		}
}