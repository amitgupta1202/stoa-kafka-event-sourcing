package com.annasystems.stoa.user.commandhandlers

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import arrow.core.right
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.CommandHandler
import com.annasystems.stoa.common.CommandResult
import com.annasystems.stoa.common.UserAlreadyExists
import com.annasystems.stoa.user.User
import com.annasystems.stoa.user.UserCommand.CreateUser
import com.annasystems.stoa.user.UserEvent.UserCreated

class CreateUserCommandHandler : CommandHandler<CreateUser, UserCreated, User> {

	@Suppress("UNUSED_VARIABLE")
	override fun handle(cmd: CreateUser, existing: Option<User>): Either<ApplicationError, CommandResult<UserCreated, User>> = Either.fx {
		val (noUser) = existing.toEither { Unit }.map { UserAlreadyExists(cmd.metadata.userId) }.swap()
		val (userEvent) = UserCreated(cmd.metadata.toEventMetadata(cmd.metadata.expectedVersion), cmd.id, cmd.version, cmd.name, cmd.emailAddress, cmd.roles).right()
		CommandResult(userEvent, userEvent.apply())
	}
}