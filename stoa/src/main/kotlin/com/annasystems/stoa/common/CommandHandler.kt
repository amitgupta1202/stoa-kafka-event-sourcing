package com.annasystems.stoa.common

import arrow.core.Either
import arrow.core.Option
import kotlinx.serialization.Serializable

interface CommandHandler<CMD, EVENT, MODEL> {
	fun handle(cmd: CMD, existing: Option<MODEL>): Either<ApplicationError, CommandResult<EVENT, MODEL>>
}

@Serializable
data class CommandResult<out EVENT, MODEL>(val event: EVENT, val model: MODEL)

