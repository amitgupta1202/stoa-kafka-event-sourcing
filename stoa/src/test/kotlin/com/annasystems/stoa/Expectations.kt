package com.annasystems.stoa

import arrow.core.extensions.set.foldable.exists
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.user.User
import org.awaitility.Awaitility
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo

internal data class Expect<T>(val command: T)

internal fun <T> T.expect() = Expect(this)

internal fun Expect<CreateSubmission>.createdSuccessfully() {
	Awaitility.await().untilCallTo { fetchSubmission(command.metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission -> submission.title == command.title }
	}
}

internal fun Expect<InviteEditor>.pendingEditorInvitation() {
	Awaitility.await().untilCallTo { fetchSubmission(command.metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editorInvitations.exists { it.user.id == command.editorId && it.state == Invitation.Companion.State.PENDING }
		}
	}
}

internal fun Expect<User.Editor>.receiveEditorInvitationEmail(submissionId: SubmissionId) {
	Awaitility.await().untilCallTo { fetchSubmission(submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editorInvitations.exists { it.user.id == command.id && it.state == Invitation.Companion.State.PENDING } &&
					submission.emails.exists { it.emailAddress == command.emailAddress && it.submissionId == submissionId }
		}
	}
}


internal fun Expect<RespondToEditorInvitation>.editorInvitationToBeAccepted() {
	Awaitility.await().untilCallTo { fetchSubmission(command.metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editor.isDefined() && submission.editor.get().id == command.editorId &&
					submission.editorInvitations.exists { it.user.id == command.editorId && it.state == Invitation.Companion.State.ACCEPTED }
		}
	}
}

internal fun  Expect<User.Editor>.pendingTaskToInviteReviewer(submissionId: SubmissionId) {
	Awaitility.await().untilCallTo { fetchSubmission(submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editor.isDefined() && submission.editor.get().id == command.id &&
					submission.editorChasers.exists { it.user.id == command.id && it.state == Chaser.Companion.State.SCHEDULED && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER }
		}
	}
}

internal fun Expect<AddReviewer>.reviewerToBeAdded() {
	Awaitility.await().untilCallTo { fetchSubmission(command.metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.reviewers.exists { it.id == command.reviewerId }
		}
	}
}

internal fun Expect<User.Editor>.taskDoneToInviteReviewer(submissionId: SubmissionId) {
	Awaitility.await().untilCallTo { fetchSubmission(submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editorChasers.exists { it.user.id == command.id && it.state == Chaser.Companion.State.CANCELLED && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER }
		}
	}
}

internal fun Expect<AssignEditor>.editorToBeAssigned() {
	Awaitility.await().untilCallTo { fetchSubmission(command.metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editor.isDefined() && submission.editor.get().id == command.editorId &&
					submission.editorChasers.exists { it.user.id == command.editorId && it.state == Chaser.Companion.State.SCHEDULED && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER }
		}
	}
}


internal fun expectChaserForEditorToInviteReviewerHasBeenSent(submissionId: SubmissionId, editor: User.Editor) {
	Awaitility.await().untilCallTo { fetchSubmission(submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.forall { submission ->
			submission.editorChasers.exists { it.user.id == editor.id && it.state == Chaser.Companion.State.SENT && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER } &&
					submission.emails.exists { it.submissionId == submission.id && it.emailType == SubmissionEmailType.CHASE_EDITOR_TO_INVITE_REVIEWER }
		}
	}
}

internal fun expectSubmissionsToBePresent(vararg submissionIds: SubmissionId) {
	val submissionIdSet = submissionIds.toSet()
	Awaitility.await().untilCallTo { fetchAllSubmissions() } matches { submissions ->
		submissions!!.filter { submissionIdSet.contains(it.id) }.size == submissionIdSet.size
	}
}
