package com.annasystems.stoa.submission

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.Configs.Serialization
import com.annasystems.stoa.common.UnexpectedError
import com.annasystems.stoa.common.doTry
import com.annasystems.stoa.submission.dao.SubmissionDao.Companion.SubmissionHistory
import com.annasystems.stoa.submission.dao.SubmissionDao.Companion.SubmissionRecord
import kotlinx.serialization.internal.ArrayListSerializer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.*

fun UUID.toSubmissionId() = SubmissionId(this)
fun String?.toSubmissionId(): Either<ApplicationError, SubmissionId> {
	val self = this
	return Either.fx {
		val (value) = Option.fromNullable(self).filter { it.isNotBlank() }.toEither { UnexpectedError(message = "submissionId cannot be blank value") }
		val (uuid) = doTry { UUID.fromString(value) }
		uuid.toSubmissionId()
	}
}

fun Submission.toJson() = doTry { Serialization.json.stringify(Submission.serializer(), this) }
fun String.toSubmission() = doTry { Serialization.json.parse(Submission.serializer(), this) }

fun List<SubmissionRecord>.toSubmissionRecordJson() = doTry { Serialization.json.stringify(ArrayListSerializer(SubmissionRecord.serializer()), this) }
fun String.toSubmissionRecords() = doTry { Serialization.json.parse(ArrayListSerializer(SubmissionRecord.serializer()), this) }
fun List<SubmissionHistory>.toSubmissionHistoryJson() = doTry { Serialization.json.stringify(ArrayListSerializer(SubmissionHistory.serializer()), this) }
fun String.toSubmissionHistories() = doTry { Serialization.json.parse(ArrayListSerializer(SubmissionHistory.serializer()), this) }

fun String.getSubmissionCommand(): Either<ApplicationError, SubmissionCommand> = doTry {
	Serialization.json.parse(
		SubmissionCommand.serializer(), this
	)
}

fun Producer<SubmissionId, SubmissionCommand>.sendCommand(topicName: String, cmd: SubmissionCommand) =
	doTry { send(ProducerRecord(topicName, cmd.metadata.submissionId, cmd)).get() }