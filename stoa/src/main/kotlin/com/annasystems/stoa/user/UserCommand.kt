package com.annasystems.stoa.user

import com.annasystems.stoa.common.EmailAddress
import com.annasystems.stoa.common.Name
import com.annasystems.stoa.common.RequestId
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed class UserCommand {
	abstract val metadata: Metadata

	@Serializable
	data class CreateUser(
		override val metadata: Metadata,
		val id: UserId,
		val version: Version,
		val name: Name,
		val emailAddress: EmailAddress,
		val roles: Set<User.Companion.Role>
	) : UserCommand()

	companion object {
		@Serializable
		data class Metadata(
			val requestId: RequestId,
			val actorId: UserId,
			@Serializable(with = InstantSerializer::class) val timestamp: Instant,
			val userId: UserId,
			val expectedVersion: Version
		)
	}
}









