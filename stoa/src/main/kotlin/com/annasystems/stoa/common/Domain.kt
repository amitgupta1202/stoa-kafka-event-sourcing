package com.annasystems.stoa.common

import com.annasystems.stoa.common.serializers.EmailAddressSerializer
import com.annasystems.stoa.common.serializers.RequestIdSerializer
import com.annasystems.stoa.common.serializers.VersionSerializer
import kotlinx.serialization.Serializable
import java.util.*

interface Id {
	val raw: UUID

	fun asString(): String = raw.toString()
}

@Serializable(with = RequestIdSerializer::class)
data class RequestId(override val raw: UUID) : Id {
	companion object {
		fun generate() = UUID.randomUUID().toRequestId()
	}
}

@Serializable(with = EmailAddressSerializer::class)
data class EmailAddress(val raw: String)

@Serializable
data class Name(val firstName: String, val lastName: String)

@Serializable(with = VersionSerializer::class)
data class Version(val num: Int) {
	fun nextVersion(): Version =
		Version(num + 1)

	companion object {
		val NONE = Version(0)
		val INITIAL = Version(1)
		val ANY = Version(-1)
	}
}

fun UUID.toRequestId() = RequestId(this)