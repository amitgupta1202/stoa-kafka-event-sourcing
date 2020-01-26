package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.right
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.CommandHandler
import com.annasystems.stoa.common.CommandResult
import com.annasystems.stoa.common.SubmissionAlreadyExists
import com.annasystems.stoa.submission.CreateSubmission
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.submission.SubmissionCreated
import com.annasystems.stoa.user.GetAuthor

class CreateSubmissionCommandHandler(
	private val getAuthor: GetAuthor
) : CommandHandler<CreateSubmission, SubmissionCreated, Submission> {

	@Suppress("UNUSED_VARIABLE")
	override fun handle(cmd: CreateSubmission, existing: Option<Submission>): Either<ApplicationError, CommandResult<SubmissionCreated, Submission>> {
		return Either.fx {
			val (noSubmission) = existing.toEither { Unit }.map { SubmissionAlreadyExists(cmd.metadata.submissionId) }.swap()
			val (author) = getAuthor(cmd.authorId)
			val (submissionEvent) = SubmissionCreated(
				cmd.metadata.toEventMetadata(cmd.metadata.expectedVersion),
				author.id,
				cmd.title,
				cmd.abstract
			).right()
			CommandResult(submissionEvent, submissionEvent.apply(author))
		}
	}
}