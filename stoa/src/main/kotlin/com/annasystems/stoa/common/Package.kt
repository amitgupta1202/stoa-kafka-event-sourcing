package com.annasystems.stoa.common

import arrow.core.*

fun <L, R> doTry(fe: (Throwable) -> L, f: () -> R): Either<L, R> =
	try {
		f().right()
	} catch (t: Throwable) {
		fe(t.nonFatalOrThrow()).left()
	}

fun <R> doTry(f: () -> R): Either<ApplicationError, R> = doTry({ UnexpectedError(throwable = it.some()) }, f)