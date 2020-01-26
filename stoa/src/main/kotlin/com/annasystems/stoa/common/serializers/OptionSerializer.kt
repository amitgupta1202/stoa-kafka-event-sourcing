package com.annasystems.stoa.common.serializers

import arrow.core.Option
import com.annasystems.stoa.user.User
import kotlinx.serialization.*
import kotlinx.serialization.internal.NullableSerializer
import kotlinx.serialization.internal.StringDescriptor

@Serializer(forClass = Option::class)
open class OptionSerializer<T>(private val kSerializer: KSerializer<T?>) : KSerializer<Option<T>> {

	override val descriptor: SerialDescriptor =
		StringDescriptor.withName("OptionDefaultSerializer")


	override fun serialize(encoder: Encoder, obj: Option<T>) {
		encoder.encode(kSerializer, obj.orNull())
	}

	override fun deserialize(decoder: Decoder): Option<T> {
		return Option.fromNullable(decoder.decode(kSerializer))
	}
}

@UseExperimental(InternalSerializationApi::class)
object EditorOptionSerializer : OptionSerializer<User.Editor>(NullableSerializer(User.Editor.serializer()))

