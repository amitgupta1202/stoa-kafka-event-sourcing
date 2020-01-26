package com.annasystems.stoa.user

import arrow.core.Either
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.CommandResult
import com.annasystems.stoa.user.UserCommand.CreateUser
import com.annasystems.stoa.user.commandhandlers.CreateUserCommandHandler

class UserCommandHandler(private val createUserCommandHandler: CreateUserCommandHandler, private val getUser: GetUser) {

	fun handle(cmd: UserCommand): Either<ApplicationError, CommandResult<UserEvent.UserCreated, User>> {
		return when (cmd) {
			is CreateUser -> createUserCommandHandler.handle(cmd, getUser(cmd.id).toOption())
		}
	}
}