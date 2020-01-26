package com.annasystems.stoa.common.serdes

import java.nio.ByteBuffer
import java.util.*

private fun toByteBuffer(uuid: UUID): ByteBuffer {
	val buffer = ByteBuffer.allocate(16)
	buffer.putLong(0, uuid.mostSignificantBits)
	buffer.putLong(8, uuid.leastSignificantBits)
	return buffer
}

private fun fromByteBuffer(buffer: ByteBuffer): UUID {
	val mostSignificantBits = buffer.getLong(0)
	val leastSignificantBits = buffer.getLong(8)
	return UUID(mostSignificantBits, leastSignificantBits)
}

internal fun UUID.toByteArray(): ByteArray = toByteBuffer(this).array()
internal fun ByteArray.toUUID(): UUID = fromByteBuffer(ByteBuffer.wrap(this))