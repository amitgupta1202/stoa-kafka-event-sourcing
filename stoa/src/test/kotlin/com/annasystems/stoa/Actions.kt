package com.annasystems.stoa

import arrow.core.Tuple4
import com.annasystems.stoa.common.EmailAddress
import com.annasystems.stoa.common.Name
import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.user.*
import org.awaitility.Awaitility
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import java.time.Instant
import java.util.*

internal fun User.Author.createSubmission(submissionId: SubmissionId): CreateSubmission {
	val metadata = SubmissionCommand.Companion.Metadata(
		RequestId.generate(),
		id,
		Instant.EPOCH,
		submissionId,
		Version.NONE
	)
	val title = Title("some title")
	val abstract = Abstract("some abstract")
	val command = CreateSubmission(
		metadata = metadata,
		authorId = id,
		title = title,
		abstract = abstract
	)
	return sendCommand(command)
}

internal fun User.EditorialAssistant.inviteEditor(editor: User.Editor, submissionId: SubmissionId): InviteEditor {
	val version = fetchSubmission(submissionId).get().version

	val requestId = RequestId.generate()
	val metadata = SubmissionCommand.Companion.Metadata(
		requestId,
		id,
		Instant.EPOCH,
		submissionId,
		version
	)
	val command = InviteEditor(metadata, editor.id)
	return sendCommand(command)
}

internal fun User.Editor.acceptInvitation(submissionId: SubmissionId): RespondToEditorInvitation {
	val version = fetchSubmission(submissionId).get().version

	val metadata = SubmissionCommand.Companion.Metadata(
		RequestId.generate(),
		id,
		Instant.EPOCH,
		submissionId,
		version
	)
	val command = RespondToEditorInvitation(
		metadata,
		id,
		SubmissionCommand.Companion.EditorInvitationResponse.ACCEPT
	)
	return sendCommand(command)
}

internal fun User.Editor.addReviewer(reviewer: User.Reviewer, submissionId: SubmissionId): AddReviewer {
	val version = fetchSubmission(submissionId).get().version

	val metadata = SubmissionCommand.Companion.Metadata(
		RequestId.generate(),
		id,
		Instant.EPOCH,
		submissionId,
		version
	)
	val command = AddReviewer(metadata, reviewer.id)
	return sendCommand(command)
}

internal fun User.EditorialAssistant.assignEditor(editor: User.Editor, submissionId: SubmissionId): AssignEditor {
	val version = fetchSubmission(submissionId).get().version

	val requestId = RequestId.generate()
	val metadata = SubmissionCommand.Companion.Metadata(
		requestId,
		id,
		Instant.EPOCH,
		submissionId,
		version
	)
	val command = AssignEditor(metadata, editor.id)
	return sendCommand(command)
}

internal fun triggerMarkTaskOverdueAsOf(taskType: TaskType, asOf: Instant) {
	sendTaskOverdue(asOf, taskType)
}

internal fun createUsers() = Tuple4(createAuthor(), createEditorialAssistant(), createEditor(), createReviewer())

private fun createAuthor(): User.Author {
	val authorId = UUID.randomUUID().toAuthorId()
	val author = User.Author(
		authorId, Version.INITIAL,
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

private fun createEditor(): User.Editor {
	val editorId = UUID.randomUUID().toEditorId()
	val editor = User.Editor(
		editorId, Version.INITIAL,
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

private fun createReviewer(): User.Reviewer {
	val reviewerId = UUID.randomUUID().toReviewerId()
	val reviewer = User.Reviewer(
		reviewerId, Version.INITIAL,
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

private fun createEditorialAssistant(): User.EditorialAssistant {
	val editorialAssistantId = UUID.randomUUID().toEditorialAssistantId()
	val editorialAssistant = User.EditorialAssistant(
		editorialAssistantId, Version.INITIAL,
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


