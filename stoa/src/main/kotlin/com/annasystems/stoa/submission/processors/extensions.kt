package com.annasystems.stoa.submission.processors

import arrow.core.Option
import arrow.core.getOrElse
import com.annasystems.stoa.common.Mapper
import org.apache.kafka.streams.kstream.ValueMapper

fun <F : Any, T> Mapper<F, Option<T>>.toIterableValueMapper(): ValueMapper<F?, Iterable<T>> {
	return ValueMapper { value ->
		checkNotNull(value)
		this.apply(value).map { listOf(it) }.getOrElse { emptyList() }
	}
}

fun <F : Any, T> Mapper<F, T>.toValueMapper(): ValueMapper<F?, T> {
	return ValueMapper { value ->
		checkNotNull(value)
		this.apply(value)
	}
}