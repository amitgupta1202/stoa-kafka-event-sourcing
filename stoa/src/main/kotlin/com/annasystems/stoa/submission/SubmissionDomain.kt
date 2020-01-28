package com.annasystems.stoa.submission

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import com.annasystems.stoa.common.*
import com.annasystems.stoa.common.serializers.EditorOptionSerializer
import com.annasystems.stoa.common.serializers.InstantSerializer
import com.annasystems.stoa.submission.serilaizers.AbstractSerializer
import com.annasystems.stoa.submission.serilaizers.SubmissionIdSerializer
import com.annasystems.stoa.submission.serilaizers.TitleSerializer
import com.annasystems.stoa.user.User
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable(with = SubmissionIdSerializer::class)
data class SubmissionId(override val raw: UUID) : Id

@Serializable(with = TitleSerializer::class)
data class Title(val raw: String)

@Serializable(with = AbstractSerializer::class)
data class Abstract(val raw: String)

@Serializable
data class Invitation<T>(
	val user: T,
	val state: State = State.PENDING
) {
	companion object {
		enum class State {
			PENDING, ACCEPTED, DECLINED
		}
	}
}

enum class TaskType {
	EDITOR_TO_INVITE_REVIEWER
}

@Serializable
data class Task<T>(
	val user: T,
	@Serializable(with = InstantSerializer::class) val overdue: Instant,
	val taskType: TaskType,
	val state: State = State.PENDING
) {
	companion object {
		enum class State {
			PENDING, OVERDUE, DONE
		}
	}
}

@Serializable
data class Submission(
	val id: SubmissionId,
	val author: User.Author,
	val title: Title,
	val abstract: Abstract,
	@Serializable(with = EditorOptionSerializer::class) val editor: Option<User.Editor> = None,
	val reviewers: Set<User.Reviewer> = emptySet(),
	val editorInvitations: Set<Invitation<User.Editor>> = emptySet(),
	val editorTasks: Set<Task<User.Editor>> = emptySet(),
	val emails: Set<SubmissionEmail> = emptySet(),
	val version: Version = Version.INITIAL
)

@Serializable
data class SubmissionEmail(
	val emailAddress: EmailAddress,
	val subject: String,
	val body: String,
	val submissionId: SubmissionId,
	val requestId: RequestId,
	@Serializable(with = InstantSerializer::class) val timestamp: Instant,
	val emailType: SubmissionEmailType
)

enum class SubmissionEmailType {
	INVITE_EDITOR, CHASE_EDITOR_TO_INVITE_REVIEWER
}

typealias GetSubmission = (SubmissionId) -> Either<ApplicationError, Submission>

