package com.annasystems.stoa

import com.annasystems.stoa.common.Logging
import com.annasystems.stoa.submission.ChaserType.EDITOR_TO_INVITE_REVIEWER
import org.awaitility.Awaitility.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant.now
import java.time.temporal.ChronoUnit.DAYS

class SubmissionWorkflowIntegrationTest : Logging {

	@Test
	fun `submission workflow from creation to reviewer assignment`() {
		val (author, editorialAssistant, editor, reviewer) = createUsers()

		createSubmission(author = author).expectSubmissionCreation()
			.inviteEditor(editor = editor, invitedBy = editorialAssistant).expectPendingEditorInvitationAndInvitationEmailBeenSent()
			.acceptInvitation(acceptedBy = editor).expectEditorInvitationToBeAcceptedAndChaserToInvitedReviewerToBeScheduled()
			.addReviewer(reviewer = reviewer, addedBy = editor).expectReviewerToBeAddedAndChaserToInviteReviewerToBeCancelled()
	}

	@Test
	fun `send a chaser email to editor when fails to invite reviewer`() {
		val (author, editorialAssistant, editor) = createUsers()
		val pretendThreeDaysHasPassed = now().plus(3, DAYS).plusMillis(1)
		val submission = createSubmission(author = author).expectSubmissionCreation()
			.assignEditor(editor = editor, invitedBy = editorialAssistant).expectEditorToBeAssignedAndChaserToInviteReviewerToBeScheduled()

		triggerChaserEmailAsOf(chaserType = EDITOR_TO_INVITE_REVIEWER, asOf = pretendThreeDaysHasPassed)

		expectChaserForEditorToInviteReviewerHasBeenSent(submission, editor)
	}

	@Test
	fun `list of submissions`() {
		val (author) = createUsers()
		val submissionA = createSubmission(author = author).metadata.submissionId
		val submissionB = createSubmission(author = author).metadata.submissionId

		await().untilCallTo { fetchAllSubmissions() } matches { submissions ->
			submissions!!.filter { it.id == submissionA || it.id == submissionB }.size == 2
		}
	}

	@Test
	fun `submission history`() {
		val (author) = createUsers()
		val submissionId = createSubmission(author = author).metadata.submissionId

		await().untilCallTo { fetchSubmissionHistory(submissionId) } matches { history ->
			history!!.isNotEmpty() && history[0].type == "Submission Created" && history[0].description == "Submission was submitted by DanielTheAuthor Jones."
		}
	}

	companion object {
		@BeforeAll
		@JvmStatic
		fun beforeAll() {
			App.start()
		}
	}
}