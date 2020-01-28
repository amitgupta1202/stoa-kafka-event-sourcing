package com.annasystems.stoa.submission.mappers

import arrow.core.Either
import arrow.core.getOrElse
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.Mapper
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.mappers.ToSubmissionHistoryTypeAndDescription.Companion.TypeAndDescription
import com.annasystems.stoa.user.AuthorId
import com.annasystems.stoa.user.EditorId
import com.annasystems.stoa.user.ReviewerId
import com.annasystems.stoa.user.User
import com.annasystems.stoa.user.dao.UserDao

class ToSubmissionHistoryTypeAndDescription(private val userDao: UserDao) : Mapper<SubmissionEvent, TypeAndDescription> {
	override fun apply(from: SubmissionEvent): TypeAndDescription =
		when (from) {
			is SubmissionCreated -> "Submission Created" to "Submission was submitted by ${from.authorId.name()}."
			is ReviewerAdded -> "Reviewer Added" to "Reviewer ${from.reviewerId.name()} was added."
			is ReviewerRemoved -> "Reviewer Removed" to "Reviewer ${from.reviewerId.name()} was removed."
			is EditorAssigned -> "Editor Assigned" to "Editor ${from.editorId.name()} was assigned."
			is EditorUnassigned -> "Editor Unassigned" to "Editor ${from.editorId.name()} was unassigned."
			is EditorInvited -> "Editor Invited" to "Editor ${from.editorId.name()} was invited."
			is EditorInvitationResponseReceived -> "Editor Invitation Response Received" to "Editor ${from.editorId.name()} response received - ${from.response}."
			is InviteEditorEmailSent -> "Invite Editor Email Sent" to "Invited email was sent to editor ${from.editorId.name()}."
			is TaskToAddReviewerCreated -> "Create Editor Task To Add Reviewer" to "Created task to to add reviewer for editor ${from.editorId.name()} overdue on ${from.chaseTime}."
			is TaskToAddReviewerChanged -> "Editor Task To Add Reviewer Action Changed" to "Task to add reviewer for editor ${from.editorId.name()} was ${from.state}."
			is EditorChaseToAddReviewerEmailSent -> "Editor Chase To Add Reviewer Email Sent" to "Chaser email was sent to editor ${from.editorId.name()}."
		}

	private fun <T : User> Either<ApplicationError, T>.name(default: String) = this.map { "${it.name.firstName} ${it.name.lastName}" }.getOrElse { default }
	private fun ReviewerId.name() = userDao.getReviewer(this).name(this.asString())
	private fun EditorId.name() = userDao.getEditor(this).name(this.asString())
	private fun AuthorId.name() = userDao.getAuthor(this).name(this.asString())

	companion object {
		data class TypeAndDescription(val eventType: String, val description: String)

		private infix fun String.to(that: String): TypeAndDescription = TypeAndDescription(this, that)
	}
}