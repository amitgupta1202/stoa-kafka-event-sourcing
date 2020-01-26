package com.annasystems.stoa.user.dao

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.flatMap
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.UserDoesNotExists
import com.annasystems.stoa.common.doTry
import com.annasystems.stoa.user.*
import redis.clients.jedis.Jedis

class UserDao(private val getUserFn: (UserId) -> Either<ApplicationError, User>) {

	fun getUser(userId: UserId): Either<ApplicationError, User> = getUserFn(userId)
	fun getAuthor(authorId: AuthorId): Either<ApplicationError, User.Author> = getUser(authorId).flatMap { it.toAuthor() }
	fun getEditor(editorId: EditorId): Either<ApplicationError, User.Editor> = getUser(editorId).flatMap { it.toEditor() }
	fun getReviewer(reviewerId: ReviewerId): Either<ApplicationError, User.Reviewer> = getUser(reviewerId).flatMap { it.toReviewer() }
	fun getEditorialAssistant(editorialAssistantId: EditorialAssistantId): Either<ApplicationError, User.EditorialAssistant> = getUser(editorialAssistantId).flatMap { it.toEditorialAssistant() }

	companion object {
		fun findUserInRedis(jedis: () -> Jedis): (UserId) -> Either<ApplicationError, User> = { userId ->
			Either.fx {
				val (userJsonNullable) = doTry { jedis().use { it.get(userId.asString()) } }
				val (userJson) = Option.fromNullable(userJsonNullable).toEither { UserDoesNotExists(userId) }
				val (user) = userJson.toUser()
				user
			}
		}
	}
}
