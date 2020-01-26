package com.annasystems.stoa.common

interface Mapper<FROM, TO> {
	fun apply(from: FROM): TO
}