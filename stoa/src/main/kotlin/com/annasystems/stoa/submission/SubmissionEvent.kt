package com.annasystems.stoa.submission

import arrow.core.none
import arrow.core.some
import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.common.serializers.InstantSerializer
import com.annasystems.stoa.submission.Task.Companion.State.*
import com.annasystems.stoa.submission.TaskType.EDITOR_TO_INVITE_REVIEWER
import com.annasystems.stoa.submission.Invitation.Companion.State.*
import com.annasystems.stoa.submission.Invitation.Companion.State.PENDING
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

		enum class TaskState {
			OVERDUE, DONE
		}
	}
}

interface EmailEvent {
	val email: SubmissionEmail
}

interface ScheduleChaseEvent {
	val chaseTime: Instant
	val taskType: TaskType
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
data class TaskToAddReviewerCreated(
	override val metadata: Metadata,
	val editorId: EditorId,
	@Serializable(with = InstantSerializer::class) override val chaseTime: Instant
) : SubmissionEvent(), ScheduleChaseEvent {

	override val taskType = EDITOR_TO_INVITE_REVIEWER

	fun apply(submission: Submission, editor: Editor): Submission {
		val remainingTasks = submission.editorTasks.filterNot { it.user == editor && it.overdue == chaseTime }.toSet()
		val tasks = Task(editor, chaseTime, taskType, Task.Companion.State.PENDING)
		return submission.copy(editorTasks = remainingTasks + tasks, version = metadata.submissionVersion)
	}
}

@Serializable
data class TaskToAddReviewerChanged(
	override val metadata: Metadata,
	val editorId: EditorId,
	@Serializable(with = InstantSerializer::class) val overdue: Instant,
	val taskType: TaskType,
	val state: SubmissionEvent.Companion.TaskState
) : SubmissionEvent() {
	fun apply(submission: Submission, editor: Editor): Submission {
		val remainingTasks = submission.editorTasks.filterNot { it.user == editor && it.overdue == overdue }.toSet()
		val tasks = when (state) {
			SubmissionEvent.Companion.TaskState.OVERDUE -> Task(editor, overdue, taskType, OVERDUE)
			SubmissionEvent.Companion.TaskState.DONE -> Task(editor, overdue, taskType, DONE)
		}
		return submission.copy(editorTasks = remainingTasks + tasks, version = metadata.submissionVersion)
	}
}

@Serializable
data class EditorChaseToAddReviewerEmailSent(
	override val metadata: Metadata,
	val editorId: EditorId,
	override val email: SubmissionEmail
) : SubmissionEvent(), EmailEvent {
	fun apply(submission: Submission): Submission {
		return submission.copy(emails = submission.emails + email, version = metadata.submissionVersion)
	}
}




