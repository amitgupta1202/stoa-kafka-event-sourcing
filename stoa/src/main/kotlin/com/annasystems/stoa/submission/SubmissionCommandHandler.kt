package com.annasystems.stoa.submission

import arrow.core.*
import arrow.core.extensions.fx
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.UnexpectedError
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.common.VersionMismatch
import com.annasystems.stoa.submission.commandhandlers.*
import com.annasystems.stoa.submission.mappers.ToSubmissionCommand
import com.annasystems.stoa.user.GetUser

class SubmissionCommandHandler(
	private val createSubmissionCommandHandler: CreateSubmissionCommandHandler,
	private val assignEditorCommandHandler: AssignEditorCommandHandler,
	private val unassignEditorCommandHandler: UnassignEditorCommandHandler,
	private val addReviewerCommandHandler: AddReviewerCommandHandler,
	private val removeReviewerCommandHandler: RemoveReviewerCommandHandler,
	private val inviteEditorCommandHandler: InviteEditorCommandHandler,
	private val respondToEditorInvitationCommandHandler: RespondToEditorInvitationCommandHandler,
	private val sendInviteEditorEmailCommandHandler: SendInviteEditorEmailCommandHandler,
	private val createEditorTaskToAddReviewerCommandHandler: CreateEditorTaskToAddReviewerCommandHandler,
	private val changeEditorTaskToAddReviewerCommandHandler: ChangeEditorTaskToAddReviewerCommandHandler,
	private val sendEditorChaseToAddReviewerEmailCommandHandler: SendEditorChaseToAddReviewerEmailCommandHandler,
	private val getUser: GetUser,
	private val getSubmission: GetSubmission
) {

	@Suppress("UNUSED_VARIABLE")
	fun handle(cmd: SubmissionCommand): Either<ApplicationError, Pair<List<SubmissionEvent>, Submission>> = Either.fx {
		val (user) = getUser(cmd.metadata.actorId)
		val submission = getSubmission(cmd.metadata.submissionId).toOption()
		val (event) = handleCommand(cmd, submission)
		event
	}

	private fun handleCommand(
		cmd: SubmissionCommand,
		submission: Option<Submission>,
		eventsTillNow: List<SubmissionEvent> = emptyList()
	): Either<ApplicationError, Pair<List<SubmissionEvent>, Submission>> {
		val result = when (submission) {
			is None ->
				when (cmd) {
					is CreateSubmission -> createSubmissionCommandHandler.handle(cmd, none())
					else -> UnexpectedError().left()
				}
			is Some -> {
				validateVersion(submission.t.version, cmd.metadata.expectedVersion).flatMap {
					when (cmd) {
						is AssignEditor -> assignEditorCommandHandler.handle(cmd, submission)
						is UnassignEditor -> unassignEditorCommandHandler.handle(cmd, submission)
						is AddReviewer -> addReviewerCommandHandler.handle(cmd, submission)
						is RemoveReviewer -> removeReviewerCommandHandler.handle(cmd, submission)
						is InviteEditor -> inviteEditorCommandHandler.handle(cmd, submission)
						is RespondToEditorInvitation -> respondToEditorInvitationCommandHandler.handle(cmd, submission)
						is SendInviteEditorEmail -> sendInviteEditorEmailCommandHandler.handle(cmd, submission)
						is CreateEditorTaskToAddReviewer -> createEditorTaskToAddReviewerCommandHandler.handle(cmd, submission)
						is ChangeEditorTaskToAddReviewer -> changeEditorTaskToAddReviewerCommandHandler.handle(cmd, submission)
						is SendEditorChaseToAddReviewerEmail -> sendEditorChaseToAddReviewerEmailCommandHandler.handle(cmd, submission)
						is CreateSubmission -> UnexpectedError().left()
					}
				}
			}
		}

		return result.flatMap { commandResult ->
			when (val newCommand = ToSubmissionCommand(commandResult.model).apply(commandResult.event)) {
				is None -> Pair(eventsTillNow + listOf(commandResult.event), commandResult.model).right()
				is Some -> handleCommand(newCommand.t, commandResult.model.some(), eventsTillNow + listOf(commandResult.event))
			}
		}
	}

	private fun validateVersion(currentVersion: Version, expectedVersion: Version) =
		Either.cond(
			currentVersion == expectedVersion || expectedVersion == Version.ANY,
			{ currentVersion },
			{ VersionMismatch(currentVersion, expectedVersion) })
}