package com.annasystems.stoa.common.serializers

import com.annasystems.stoa.common.EmailAddress
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = EmailAddress::class)
object EmailAddressSerializer : KSerializer<EmailAddress> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("EmailDefaultSerializer")

	override fun serialize(encoder: Encoder, obj: EmailAddress) {
		encoder.encodeString(obj.raw)
	}

	override fun deserialize(decoder: Decoder): EmailAddress {
		return EmailAddress(decoder.decodeString())
	}
}