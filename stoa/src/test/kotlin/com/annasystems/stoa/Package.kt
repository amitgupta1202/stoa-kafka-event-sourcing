package com.annasystems.stoa

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.set.foldable.exists
import arrow.core.getOrElse
import com.annasystems.stoa.common.Configs
import com.annasystems.stoa.common.Utils.createProducer
import com.annasystems.stoa.common.EmailAddress
import com.annasystems.stoa.common.Name
import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.submission.dao.SubmissionDao
import com.annasystems.stoa.submission.dao.SubmissionDao.Companion.SubmissionRecord
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.user.*
import com.annasystems.stoa.user.dao.UserDao
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import java.time.Instant
import java.util.*

private val userProducer = createProducer(Configs.bootstrapServers, Configs.Topics.userCommands)
private val submissionChaserTriggerProducer = createProducer(Configs.bootstrapServers, Configs.Topics.submissionChaseTrigger)

private val userDao = UserDao(UserDao.findUserInRedis { Configs.Redis.jedisPool.resource })
private val client: HttpHandler = OkHttp()

internal fun <T> Option<T>.get() = this.getOrElse { throw RuntimeException("value not defined") }
internal fun <L, R> Either<L, R>.get() = this.getOrElse { throw RuntimeException("value not defined") }

internal fun sendCommand(cmd: UserCommand) {
	userProducer.send(ProducerRecord(Configs.Topics.userCommands.name, cmd.metadata.userId, cmd)).get()
}

internal fun sendChaser(asOf: Instant, chaserType: ChaserType) {
	submissionChaserTriggerProducer.send(ProducerRecord(Configs.Topics.submissionChaseTrigger.name, asOf.toEpochMilli(), chaserType.toString())).get()
}

internal fun createUsers() = TestUsers()

internal data class TestUsers(
	val author: User.Author = createAuthor(),
	val editorialAssistant: User.EditorialAssistant = createEditorialAssistant(),
	val editor: User.Editor = createEditor(),
	val reviewer: User.Reviewer = createReviewer()
)

internal fun createAuthor(): User.Author {
	val authorId = UUID.randomUUID().toAuthorId()
	val author = User.Author(authorId, Version.INITIAL,
		Name("DanielTheAuthor", "Jones"),
		EmailAddress("DanielTheAuthor@email.com")
	)
	val authorMetadata = UserCommand.Companion.Metadata(
		RequestId.generate(),
		authorId,
		Instant.EPOCH,
		authorId,
		Version.NONE
	)
	sendCommand(UserCommand.CreateUser(authorMetadata, authorId, Version.NONE, author.name, author.emailAddress, author.roles))
	Awaitility.await().untilCallTo { userDao.getAuthor(authorId).toOption() } matches { mayBeAuthor -> mayBeAuthor!!.isDefined() }
	return userDao.getAuthor(authorId).get()
}

internal fun createEditor(): User.Editor {
	val editorId = UUID.randomUUID().toEditorId()
	val editor = User.Editor(editorId, Version.INITIAL,
		Name("BenTheEditor", "Gillies"),
		EmailAddress("BenTheEditor@email.com")
	)
	val editorMetadata = UserCommand.Companion.Metadata(
		RequestId.generate(),
		editorId,
		Instant.EPOCH,
		editorId,
		Version.NONE
	)
	sendCommand(UserCommand.CreateUser(editorMetadata, editorId, Version.NONE, editor.name, editor.emailAddress, editor.roles))
	Awaitility.await().untilCallTo { userDao.getEditor(editorId).toOption() } matches { mayBeEditor -> mayBeEditor!!.isDefined() }
	return userDao.getEditor(editorId).get()
}

internal fun createReviewer(): User.Reviewer {
	val reviewerId = UUID.randomUUID().toReviewerId()
	val reviewer = User.Reviewer(reviewerId, Version.INITIAL,
		Name("PaulTheReviewer", "Mac"),
		EmailAddress("PaulTheReviewer@email.com")
	)
	val reviewerMetadata = UserCommand.Companion.Metadata(
		RequestId.generate(),
		reviewerId,
		Instant.EPOCH,
		reviewerId,
		Version.NONE
	)
	sendCommand(UserCommand.CreateUser(reviewerMetadata, reviewerId, Version.NONE, reviewer.name, reviewer.emailAddress, reviewer.roles))
	Awaitility.await().untilCallTo { userDao.getReviewer(reviewerId).toOption() } matches { mayBeReviewer -> mayBeReviewer!!.isDefined() }
	return userDao.getReviewer(reviewerId).get()
}

internal fun createEditorialAssistant(): User.EditorialAssistant {
	val editorialAssistantId = UUID.randomUUID().toEditorialAssistantId()
	val editorialAssistant = User.EditorialAssistant(editorialAssistantId, Version.INITIAL,
		Name("JulianTheEditorialAssistant", "Hamilton"),
		EmailAddress("JulianTheEditorialAssistant@email.com")
	)
	val editorialAssistantMetadata = UserCommand.Companion.Metadata(
		RequestId.generate(),
		editorialAssistantId,
		Instant.EPOCH,
		editorialAssistantId,
		Version.NONE
	)
	sendCommand(
		UserCommand.CreateUser(
			editorialAssistantMetadata,
			editorialAssistantId,
			Version.NONE,
			editorialAssistant.name,
			editorialAssistant.emailAddress,
			editorialAssistant.roles
		)
	)
	Awaitility.await().untilCallTo { userDao.getEditorialAssistant(editorialAssistantId).toOption() } matches { mayBeEditorialAssistant -> mayBeEditorialAssistant!!.isDefined() }
	return userDao.getEditorialAssistant(editorialAssistantId).get()
}

internal fun createSubmission(author: User.Author): CreateSubmission {
	val submissionId = UUID.randomUUID().toSubmissionId()
	val metadata = SubmissionCommand.Companion.Metadata(
		RequestId.generate(),
		author.id,
		Instant.EPOCH,
		submissionId,
		Version.NONE
	)
	val title = Title("some title")
	val abstract = Abstract("some abstract")
	val command = CreateSubmission(
		metadata = metadata,
		authorId = author.id,
		title = title,
		abstract = abstract
	)
	sendCommand(command)
	return command
}

internal fun CreateSubmission.expectSubmissionCreation(): Submission {
	Awaitility.await().untilCallTo { fetchSubmission(metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission -> submission.title == title }
	}
	return fetchSubmission(metadata.submissionId).get()
}

internal fun Submission.inviteEditor(editor: User.Editor, invitedBy: User): InviteEditor {
	val submissionId = this.id
	val requestId = RequestId.generate()
	val metadata = SubmissionCommand.Companion.Metadata(
		requestId,
		invitedBy.id,
		Instant.EPOCH,
		submissionId,
		this.version
	)
	val command = InviteEditor(metadata, editor.id)
	sendCommand(command)
	return command
}

internal fun InviteEditor.expectPendingEditorInvitationAndInvitationEmailBeenSent(): Submission {
	Awaitility.await().untilCallTo { fetchSubmission(metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editorInvitations.exists { it.user.id == editorId && it.state == Invitation.Companion.State.PENDING } &&
					submission.emails.exists { it.requestId == metadata.requestId && it.submissionId == metadata.submissionId }
		}
	}
	return fetchSubmission(this.metadata.submissionId).get()
}

internal fun Submission.acceptInvitation(acceptedBy: User.Editor): RespondToEditorInvitation {
	val submissionId = id
	val metadata = SubmissionCommand.Companion.Metadata(
		RequestId.generate(),
		acceptedBy.id,
		Instant.EPOCH,
		submissionId,
		this.version
	)
	val command = RespondToEditorInvitation(
		metadata,
		acceptedBy.id,
		SubmissionCommand.Companion.EditorInvitationResponse.ACCEPT
	)
	sendCommand(command)
	return command

}

internal fun RespondToEditorInvitation.expectEditorInvitationToBeAcceptedAndChaserToInvitedReviewerToBeScheduled(): Submission {
	Awaitility.await().untilCallTo { fetchSubmission(metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editor.isDefined() && submission.editor.get().id == editorId &&
					submission.editorInvitations.exists { it.user.id == editorId && it.state == Invitation.Companion.State.ACCEPTED } &&
					submission.editorChasers.exists { it.user.id == editorId && it.state == Chaser.Companion.State.SCHEDULED && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER }
		}
	}
	return fetchSubmission(metadata.submissionId).get()
}

internal fun Submission.addReviewer(reviewer: User.Reviewer, addedBy: User): AddReviewer {
	val submissionId = this.id
	val metadata = SubmissionCommand.Companion.Metadata(
		RequestId.generate(),
		addedBy.id,
		Instant.EPOCH,
		submissionId,
		this.version
	)
	val command = AddReviewer(metadata, reviewer.id)
	sendCommand(command)
	return command
}

internal fun AddReviewer.expectReviewerToBeAddedAndChaserToInviteReviewerToBeCancelled(): Submission {
	Awaitility.await().untilCallTo { fetchSubmission(metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.reviewers.exists { it.id == reviewerId } &&
					submission.editorChasers.exists { it.user.id == metadata.actorId && it.state == Chaser.Companion.State.CANCELLED && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER }
		}
	}
	return fetchSubmission(metadata.submissionId).get()
}

internal fun Submission.assignEditor(editor: User.Editor, invitedBy: User): AssignEditor {
	val submissionId = this.id
	val requestId = RequestId.generate()
	val metadata = SubmissionCommand.Companion.Metadata(
		requestId,
		invitedBy.id,
		Instant.EPOCH,
		submissionId,
		this.version
	)
	val command = AssignEditor(metadata, editor.id)
	sendCommand(command)
	return command
}

internal fun AssignEditor.expectEditorToBeAssignedAndChaserToInviteReviewerToBeScheduled(): Submission {
	Awaitility.await().untilCallTo { fetchSubmission(metadata.submissionId) } matches { maybeSubmission ->
		maybeSubmission!!.isDefined() && maybeSubmission.forall { submission ->
			submission.editor.isDefined() && submission.editor.get().id == editorId &&
					submission.editorChasers.exists { it.user.id == editorId && it.state == Chaser.Companion.State.SCHEDULED && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER }
		}
	}
	return fetchSubmission(metadata.submissionId).get()
}

internal fun triggerChaserEmailAsOf(chaserType: ChaserType, asOf: Instant) {
	sendChaser(asOf, chaserType)
}

internal fun expectChaserForEditorToInviteReviewerHasBeenSent(submission: Submission, editor: User.Editor): Submission {
	Awaitility.await().untilCallTo { fetchSubmission(submission.id) } matches { maybeSubmission ->
		maybeSubmission!!.forall { submission ->
			submission.editorChasers.exists { it.user.id == editor.id && it.state == Chaser.Companion.State.SENT && it.chaseType == ChaserType.EDITOR_TO_INVITE_REVIEWER } &&
					submission.emails.exists { it.submissionId == submission.id && it.emailType == SubmissionEmailType.CHASE_EDITOR_TO_INVITE_REVIEWER }
		}
	}
	return fetchSubmission(submission.id).get()
}

internal fun fetchSubmission(submissionId: SubmissionId): Option<Submission> {
	val networkResponse: Response = client(Request(Method.GET, "http://localhost:9000/submissions/${submissionId.asString()}"))
	return networkResponse.bodyString().toSubmission().toOption()
}

internal fun fetchAllSubmissions(): List<SubmissionRecord> {
	val networkResponse: Response = client(Request(Method.GET, "http://localhost:9000/submissions"))
	return networkResponse.bodyString().toSubmissionRecords().get()
}

internal fun fetchSubmissionHistory(submissionId: SubmissionId): List<SubmissionDao.Companion.SubmissionHistory> {
	val networkResponse: Response = client(Request(Method.GET, "http://localhost:9000/submissions/${submissionId.asString()}/history"))
	return networkResponse.bodyString().toSubmissionHistories().get()
}

object CommandSent
internal fun sendCommand(cmd: SubmissionCommand): CommandSent {
	val json = Configs.Serialization.json.stringify(SubmissionCommand.serializer(), cmd)
	client(Request(Method.POST, "http://localhost:9000/submissions/async").body(json))
	return CommandSent
}
