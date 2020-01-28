package com.annasystems.stoa.submission

import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.common.serializers.InstantSerializer
import com.annasystems.stoa.submission.SubmissionCommand.Companion.Metadata
import com.annasystems.stoa.user.AuthorId
import com.annasystems.stoa.user.EditorId
import com.annasystems.stoa.user.ReviewerId
import com.annasystems.stoa.user.UserId
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed class SubmissionCommand {
	abstract val metadata: Metadata

	companion object {
		@Serializable
		data class Metadata(
			val requestId: RequestId,
			val actorId: UserId,
			@Serializable(with = InstantSerializer::class) val timestamp: Instant,
			val submissionId: SubmissionId,
			val expectedVersion: Version
		)

		enum class EditorInvitationResponse {
			ACCEPT, DECLINE
		}

		enum class TaskState {
			OVERDUE, DONE
		}
	}
}

@Serializable
data class CreateSubmission(
	override val metadata: Metadata,
	val authorId: AuthorId,
	val title: Title,
	val abstract: Abstract
) : SubmissionCommand()

@Serializable
data class AddReviewer(
	override val metadata: Metadata,
	val reviewerId: ReviewerId
) : SubmissionCommand()

@Serializable
data class RemoveReviewer(
	override val metadata: Metadata,
	val reviewerId: ReviewerId
) : SubmissionCommand()

@Serializable
data class AssignEditor(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionCommand()

@Serializable
data class UnassignEditor(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionCommand()

@Serializable
data class InviteEditor(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionCommand()

@Serializable
data class SendInviteEditorEmail(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionCommand()

@Serializable
data class RespondToEditorInvitation(
	override val metadata: Metadata,
	val editorId: EditorId,
	val response: SubmissionCommand.Companion.EditorInvitationResponse
) : SubmissionCommand()

@Serializable
data class CreateEditorTaskToAddReviewer(
	override val metadata: Metadata,
	val editorId: EditorId,
	@Serializable(with = InstantSerializer::class) val overdue: Instant
) : SubmissionCommand()

@Serializable
data class ChangeEditorTaskToAddReviewer(
	override val metadata: Metadata,
	val editorId: EditorId, @Serializable(with = InstantSerializer::class) val overdue: Instant,
	val state: SubmissionCommand.Companion.TaskState
) : SubmissionCommand()

@Serializable
data class SendEditorChaseToAddReviewerEmail(
	override val metadata: Metadata,
	val editorId: EditorId
) : SubmissionCommand()








