package com.annasystems.stoa.user

import arrow.core.Either
import com.annasystems.stoa.common.*
import com.annasystems.stoa.user.User.*
import com.annasystems.stoa.user.User.Companion.Role.*
import com.annasystems.stoa.user.serializers.AuthorIdSerializer
import com.annasystems.stoa.user.serializers.EditorIdSerializer
import com.annasystems.stoa.user.serializers.ReviewerIdSerializer
import com.annasystems.stoa.user.serializers.UserIdSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable(with = UserIdSerializer::class)
sealed class UserId : Id

@Serializable(with = AuthorIdSerializer::class)
data class AuthorId(override val raw: UUID) : UserId()

@Serializable(with = ReviewerIdSerializer::class)
data class ReviewerId(override val raw: UUID) : UserId()

@Serializable(with = EditorIdSerializer::class)
data class EditorId(override val raw: UUID) : UserId()

@Serializable(with = EditorIdSerializer::class)
data class EditorialAssistantId(override val raw: UUID) : UserId()

@Serializable(with = UserIdSerializer::class)
data class DefaultUserId(override val raw: UUID) : UserId()

@Serializable
sealed class User {
	abstract val id: UserId
	abstract val version: Version
	abstract val name: Name
	abstract val emailAddress: EmailAddress
	abstract val roles: Set<Role>

	@Serializable
	data class Reviewer(override val id: ReviewerId, override val version: Version, override val name: Name, override val emailAddress: EmailAddress) : User() {
		override val roles: Set<Role>
			get() = setOf(REVIEWER)
	}

	@Serializable
	data class Author(override val id: AuthorId, override val version: Version, override val name: Name, override val emailAddress: EmailAddress) : User() {
		override val roles: Set<Role>
			get() = setOf(AUTHOR)
	}

	@Serializable
	data class Editor(override val id: EditorId, override val version: Version, override val name: Name, override val emailAddress: EmailAddress) : User() {
		override val roles: Set<Role>
			get() = setOf(EDITOR)
	}

	@Serializable
	data class EditorialAssistant(override val id: EditorialAssistantId, override val version: Version, override val name: Name, override val emailAddress: EmailAddress) : User() {
		override val roles: Set<Role>
			get() = setOf(EDITORIAL_ASSISTANT)
	}

	@Serializable
	data class DefaultUser(override val id: UserId, override val version: Version, override val name: Name, override val emailAddress: EmailAddress, override val roles: Set<Role>) : User()

	companion object {
		enum class Role {
			REVIEWER, AUTHOR, EDITOR, EDITORIAL_ASSISTANT
		}
	}
}

typealias GetUser = (UserId) -> Either<ApplicationError, User>
typealias GetAuthor = (AuthorId) -> Either<ApplicationError, Author>
typealias GetEditor = (EditorId) -> Either<ApplicationError, Editor>
typealias GetReviewer = (ReviewerId) -> Either<ApplicationError, Reviewer>

