package com.annasystems.stoa.common.serializers

import com.annasystems.stoa.common.Version
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = Version::class)
object VersionSerializer : KSerializer<Version> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("VersionDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: Version) {
		encoder.encodeInt(obj.num)
	}

	override fun deserialize(decoder: Decoder): Version {
		return Version(decoder.decodeInt())
	}
}