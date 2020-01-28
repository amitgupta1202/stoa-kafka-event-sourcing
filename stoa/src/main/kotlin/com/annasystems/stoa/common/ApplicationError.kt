package com.annasystems.stoa.common

import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.getOrElse
import arrow.core.none
import com.annasystems.stoa.common.serializers.InstantSerializer
import com.annasystems.stoa.submission.SubmissionId
import com.annasystems.stoa.user.EditorId
import com.annasystems.stoa.user.ReviewerId
import com.annasystems.stoa.user.User
import com.annasystems.stoa.user.UserId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant

@Serializable
sealed class ApplicationError {
	abstract fun message(): String
}


@Serializable
data class UnexpectedError(@Transient private val throwable: Option<Throwable> = none(), private val message: String = "Unexpected error") : ApplicationError() {
	override fun message(): String =
		Option.fx {
			val (error) = throwable
			val (message) = Option.fromNullable(error.message)
			message
		}.getOrElse { message }
}

@Serializable
data class VersionMismatch(val actual: Version, val expected: Version) : ApplicationError() {
	override fun message(): String =
		"Version mismatch - expected $expected, actually $actual"
}

@Serializable
data class SubmissionAlreadyExists(val submissionId: SubmissionId) : ApplicationError() {
	override fun message(): String = "Submission already exists with id = $submissionId."
}

@Serializable
data class SubmissionDoesNotExists(val submissionId: SubmissionId) : ApplicationError() {
	override fun message(): String = "Submission does not exists with id = $submissionId."
}

@Serializable
data class ReviewerForSubmissionAlreadyExists(val submissionId: SubmissionId, val reviewer: ReviewerId) : ApplicationError() {
	override fun message(): String =
		"Reviewer = $reviewer does already exists for submission id = $submissionId."
}

@Serializable
data class ReviewerForSubmissionDoesNotExists(val submissionId: SubmissionId, val reviewer: ReviewerId) : ApplicationError() {
	override fun message(): String =
		"Reviewer = $reviewer does not exists for submission id = $submissionId."
}

@Serializable
data class EditorAlreadyAssigned(val submissionId: SubmissionId, val editor: EditorId) : ApplicationError() {
	override fun message(): String =
		"Editor = $editor already exists for submission id = $submissionId."
}

@Serializable
data class EditorAlreadyInvited(val submissionId: SubmissionId, val editor: EditorId) : ApplicationError() {
	override fun message(): String =
		"Editor = $editor already invited for submission id = $submissionId."
}

@Serializable
data class EditorAlreadyHasPendingTask(val submissionId: SubmissionId, val editor: EditorId, @Serializable(with = InstantSerializer::class) val chaseTime: Instant) : ApplicationError() {
	override fun message(): String =
		"Editor = $editor already has pending task for submission id = $submissionId at ${chaseTime}."
}

@Serializable
data class EditorHasNoPendingTask(val submissionId: SubmissionId, val editor: EditorId, @Serializable(with = InstantSerializer::class) val chaseTime: Instant) : ApplicationError() {
	override fun message(): String =
		"Editor = $editor has no pending task for submission id = $submissionId at ${chaseTime}."
}

@Serializable
data class EditorHasNoPendingInvitation(val submissionId: SubmissionId, val editor: EditorId) : ApplicationError() {
	override fun message(): String =
		"Editor = $editor has no pending invitation for submission id = $submissionId."
}

@Serializable
data class EditorNotAssigned(val submissionId: SubmissionId, val editor: EditorId) : ApplicationError() {
	override fun message(): String =
		"Editor = $editor already exists for submission id = $submissionId."
}

@Serializable
data class UserDoesNotExists(val userId: UserId) : ApplicationError() {
	override fun message(): String =
		"No registered user found for id = $userId."
}

@Serializable
data class UserAlreadyExists(val userId: UserId) : ApplicationError() {
	override fun message(): String =
		"Registered user already exists for id = $userId."
}

@Serializable
data class UserNotAReviewer(val user: User) : ApplicationError() {
	override fun message(): String = "$user is not a reviewer."
}

@Serializable
data class UserNotAnEditor(val user: User) : ApplicationError() {
	override fun message(): String = "$user is not an editor."
}

@Serializable
data class UserNotAnAuthor(val user: User) : ApplicationError() {
	override fun message(): String = "$user is not an author."
}

@Serializable
data class UserNotAnEditorialAssistant(val user: User) : ApplicationError() {
	override fun message(): String = "$user is not an editorial assistant."
}

