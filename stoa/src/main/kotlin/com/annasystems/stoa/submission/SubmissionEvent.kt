package com.annasystems.stoa.submission

import arrow.core.none
import arrow.core.some
import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.common.serializers.InstantSerializer
import com.annasystems.stoa.submission.Chaser.Companion.State.*
import com.annasystems.stoa.submission.ChaserType.EDITOR_TO_INVITE_REVIEWER
import com.annasystems.stoa.submission.Invitation.Companion.State.*
import com.annasystems.stoa.submission.SubmissionEvent.Companion.EditorInvitationResponse
import com.annasystems.stoa.submission.SubmissionEvent.Companion.Metadata
import com.annasystems.stoa.user.AuthorId
import com.annasystems.stoa.user.EditorId
import com.annasystems.stoa.user.ReviewerId
import com.annasystems.stoa.user.User.*
import com.annasystems.stoa.user.UserId
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed class SubmissionEvent {
	abstract val metadata: Metadata

	companion object {
		@Serializable
		data class Metadata(
			val requestId: RequestId,
			val actorId: UserId,
			@Serializable(with = InstantSerializer::class) val timestamp: Instant,
			val submissionId: SubmissionId,
			val submissionVersion: Version
		)

		enum class EditorInvitationResponse {
			ACCEPTED, DECLINED
		}

		enum class ChaserAction {
			SENT, CANCELLED
		}
	}
}

interface EmailEvent {
	val email: SubmissionEmail
}

interface ScheduleChaseEvent {
	val chaseTime: Instant
	val chaserType: ChaserType
}

@Serializable
data class SubmissionCreated(
	override val metadata: Metadata,
	val authorId: AuthorId,
	val title: Title,
	val abstract: Abstract
) : SubmissionEvent() {
	fun apply(author: Author) = Submission(metadata.submissionId, author, title, abstract, version = metadata.submissionVersion)
}

@Serializable
data class ReviewerAdded(
	override val metadata: Metadata,
	val reviewerId: ReviewerId
) : SubmissionEvent() {
	fun apply(submission: Submission, reviewer: Reviewer) = submission.copy(reviewers = submission.reviewers + reviewer, version = metadata.submissionVersion)
}

@Serializable
data class ReviewerRemoved(
	override val metadata: Metadata,
	val reviewerId: ReviewerId
) : SubmissionEvent() {
	fun apply(submission: Submission, reviewer: Reviewer) = submission.copy(reviewers = submission.reviewers - reviewer, version = metadata.submissionVersion)
}

@Serializable
data class EditorAssigned(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionEvent()

{
	fun apply(submission: Submission, editor: Editor) = submission.copy(editor = editor.some(), version = metadata.submissionVersion)
}

@Serializable
data class EditorUnassigned(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionEvent() {
	fun apply(submission: Submission): Submission {
		return submission.copy(editor = none(), version = metadata.submissionVersion)
	}
}

@Serializable
data class EditorInvited(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionEvent() {
	fun apply(submission: Submission, editor: Editor): Submission {
		val remainingInvitations = submission.editorInvitations.filterNot { it.user.id == editor.id }.toSet()
		val invitation = Invitation(editor, PENDING)
		return submission.copy(editorInvitations = remainingInvitations + invitation, version = metadata.submissionVersion)
	}
}

@Serializable
data class EditorInvitationResponseReceived(
	override val metadata: Metadata,
	val editorId: EditorId,
	val response: EditorInvitationResponse
) : SubmissionEvent() {
	fun apply(submission: Submission, editor: Editor): Submission {
		val remainingInvitations = submission.editorInvitations.filterNot { it.user.id == editor.id }.toSet()
		val invitation = when (response) {
			EditorInvitationResponse.ACCEPTED -> Invitation(editor, ACCEPTED)
			EditorInvitationResponse.DECLINED -> Invitation(editor, DECLINED)
		}
		return submission.copy(editorInvitations = remainingInvitations + invitation, version = metadata.submissionVersion)
	}
}

@Serializable
data class InviteEditorEmailSent(
	override val metadata: Metadata,
	val editorId: EditorId,
	override val email: SubmissionEmail
) : SubmissionEvent(), EmailEvent {
	fun apply(submission: Submission): Submission {
		return submission.copy(emails = submission.emails + email, version = metadata.submissionVersion)
	}
}

@Serializable
data class ScheduledEditorChaseToInviteReviewer(
	override val metadata: Metadata,
	val editorId: EditorId,
	@Serializable(with = InstantSerializer::class) override val chaseTime: Instant
) : SubmissionEvent(), ScheduleChaseEvent {

	override val chaserType = EDITOR_TO_INVITE_REVIEWER

	fun apply(submission: Submission, editor: Editor): Submission {
		val remainingChasers = submission.editorChasers.filterNot { it.user == editor && it.chaseTime == chaseTime }.toSet()
		val chasers = Chaser(editor, chaseTime, chaserType, SCHEDULED)
		return submission.copy(editorChasers = remainingChasers + chasers, version = metadata.submissionVersion)
	}
}

@Serializable
data class EditorChaseToInviteReviewerActionReceived(
	override val metadata: Metadata,
	val editorId: EditorId,
	@Serializable(with = InstantSerializer::class) val chaseTime: Instant,
	val chaserType: ChaserType,
	val action: SubmissionEvent.Companion.ChaserAction
) : SubmissionEvent() {
	fun apply(submission: Submission, editor: Editor): Submission {
		val remainingChasers = submission.editorChasers.filterNot { it.user == editor && it.chaseTime == chaseTime }.toSet()
		val chasers = when (action) {
			SubmissionEvent.Companion.ChaserAction.SENT -> Chaser(editor, chaseTime, chaserType, SENT)
			SubmissionEvent.Companion.ChaserAction.CANCELLED -> Chaser(
				editor,
				chaseTime,
				chaserType,
				CANCELLED
			)
		}
		return submission.copy(editorChasers = remainingChasers + chasers, version = metadata.submissionVersion)
	}
}

@Serializable
data class EditorChaseToInviteReviewerEmailSent(
	override val metadata: Metadata,
	val editorId: EditorId,
	override val email: SubmissionEmail
) : SubmissionEvent(), EmailEvent {
	fun apply(submission: Submission): Submission {
		return submission.copy(emails = submission.emails + email, version = metadata.submissionVersion)
	}
}




