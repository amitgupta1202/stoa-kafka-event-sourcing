package com.annasystems.stoa

import com.annasystems.stoa.common.Logging
import com.annasystems.stoa.submission.TaskType.EDITOR_TO_ADD_REVIEWER
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant.now
import java.time.temporal.ChronoUnit.DAYS

class SubmissionWorkflowIntegrationTest : Logging {

	@Test
	fun `submission creation to reviewer assignment`() {
		val (author, editorialAssistant, editor, reviewer) = createUsers()
		val submissionId = randomSubmissionId()

		author.createSubmission(submissionId).expect()
			.createdSuccessfully()

		editorialAssistant.inviteEditor(editor, submissionId).expect()
			.pendingEditorInvitation()

		editor.expect()
			.receiveEditorInvitationEmail(submissionId)

		editor.acceptInvitation(submissionId).expect()
			.editorInvitationToBeAccepted()

		editor.expect()
			.pendingTaskToAddReviewer(submissionId)

		editor.addReviewer(reviewer, submissionId).expect()
			.reviewerToBeAdded()

		editor.expect()
			.taskDoneToAddReviewer(submissionId)
	}

	@Test
	fun `send a chaser email to editor when fails to invite reviewer`() {
		val (author, editorialAssistant, editor) = createUsers()
		val submissionId = randomSubmissionId()
		val threeDaysLater = now().plus(3, DAYS).plusMillis(1)

		author.createSubmission(submissionId).expect()
			.createdSuccessfully()

		editorialAssistant.assignEditor(editor, submissionId).expect()
			.editorToBeAssigned()

		editor.expect()
			.pendingTaskToAddReviewer(submissionId)

		triggerMarkTaskOverdueAsOf(taskType = EDITOR_TO_ADD_REVIEWER, asOf = threeDaysLater)

		editor.expect()
			.receiveChaserEmailForEditorToAddReviewer(submissionId)
	}

	@Test
	fun `list of submissions`() {
		val (author) = createUsers()
		val submissionIdA = randomSubmissionId()
		val submissionIdB = randomSubmissionId()

		author.createSubmission(submissionIdA)
		author.createSubmission(submissionIdB)

		expectSubmissionsToBePresent(submissionIdA, submissionIdB)
	}

	@Test
	fun `submission history`() {
		val (author) = createUsers()
		val submissionId = randomSubmissionId()

		author.createSubmission(submissionId)

		await().untilCallTo { fetchSubmissionHistory(submissionId) } matches { history ->
			history!!.isNotEmpty() && history[0].type == "Submission Created" && history[0].description == "Submission was submitted by DanielTheAuthor Jones."
		}
	}


	companion object {
		@BeforeAll
		@JvmStatic
		fun beforeAll() {
			checkHttpServerIsRunning().mapLeft {
				App.start()
			}
		}
	}
}