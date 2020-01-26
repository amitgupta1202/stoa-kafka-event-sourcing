package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.AddReviewer
import com.annasystems.stoa.submission.ReviewerAdded
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.user.GetReviewer

class AddReviewerCommandHandler(
	private val getReviewer: GetReviewer
) : CommandHandler<AddReviewer, ReviewerAdded, Submission> {

	override fun handle(cmd: AddReviewer, existing: Option<Submission>): Either<ApplicationError, CommandResult<ReviewerAdded, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (reviewer) = getReviewer(cmd.reviewerId)
		val (submissionEvent) = Either.cond(
			!submission.reviewers.contains(reviewer),
			{ ReviewerAdded(cmd.metadata.toEventMetadata(submission.version), cmd.reviewerId) },
			{ ReviewerForSubmissionAlreadyExists(submission.id, cmd.reviewerId) })
		CommandResult(submissionEvent, submissionEvent.apply(submission, reviewer))
	}
}