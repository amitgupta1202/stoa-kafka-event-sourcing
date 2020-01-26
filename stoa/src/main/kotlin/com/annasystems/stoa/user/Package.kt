package com.annasystems.stoa.user

import arrow.core.Either
import com.annasystems.stoa.common.*
import com.annasystems.stoa.common.Configs.Serialization
import java.util.*

fun UUID.toAuthorId() = AuthorId(this)
fun UUID.toReviewerId() = ReviewerId(this)
fun UUID.toEditorId() = EditorId(this)
fun UUID.toEditorialAssistantId() = EditorialAssistantId(this)
fun UUID.toUserId() = DefaultUserId(this)

fun User.toReviewer(): Either<UserNotAReviewer, User.Reviewer> =
	Either.cond(
		this.roles.contains(User.Companion.Role.REVIEWER),
		{ User.Reviewer(id.raw.toReviewerId(), version, name, emailAddress) },
		{ UserNotAReviewer(this) })

fun User.toAuthor(): Either<UserNotAnAuthor, User.Author> =
	Either.cond(
		this.roles.contains(User.Companion.Role.AUTHOR),
		{ User.Author(id.raw.toAuthorId(), version, name, emailAddress) },
		{ UserNotAnAuthor(this) })

fun User.toEditor(): Either<UserNotAnEditor, User.Editor> =
	Either.cond(
		this.roles.contains(User.Companion.Role.EDITOR),
		{ User.Editor(id.raw.toEditorId(), version, name, emailAddress) },
		{ UserNotAnEditor(this) })

fun User.toEditorialAssistant(): Either<UserNotAnEditorialAssistant, User.EditorialAssistant> =
	Either.cond(
		this.roles.contains(User.Companion.Role.EDITORIAL_ASSISTANT),
		{ User.EditorialAssistant(id.raw.toEditorialAssistantId(), version, name, emailAddress) },
		{ UserNotAnEditorialAssistant(this) })


fun User.toJson() = doTry { Serialization.json.stringify(User.serializer(), this) }
fun String.toUser() = doTry { Serialization.json.parse(User.serializer(), this) }
