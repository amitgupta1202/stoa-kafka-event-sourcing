package com.annasystems.stoa.submission.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.*
import com.annasystems.stoa.submission.RemoveReviewer
import com.annasystems.stoa.submission.ReviewerRemoved
import com.annasystems.stoa.submission.Submission
import com.annasystems.stoa.user.GetReviewer

class RemoveReviewerCommandHandler(
	private val getReviewer: GetReviewer
) : CommandHandler<RemoveReviewer, ReviewerRemoved, Submission> {

	override fun handle(cmd: RemoveReviewer, existing: Option<Submission>): Either<ApplicationError, CommandResult<ReviewerRemoved, Submission>> = Either.fx {
		val (submission) = existing.toEither { SubmissionDoesNotExists(cmd.metadata.submissionId) }
		val (reviewer) = getReviewer(cmd.reviewerId)
		val (submissionEvent) = Either.cond(
			submission.reviewers.contains(reviewer),
			{ ReviewerRemoved(cmd.metadata.toEventMetadata(submission.version), cmd.reviewerId) },
			{ ReviewerForSubmissionDoesNotExists(submission.id, cmd.reviewerId) })
		CommandResult(submissionEvent, submissionEvent.apply(submission, reviewer))
	}
}