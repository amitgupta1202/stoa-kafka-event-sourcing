package com.annasystems.stoa.submission.dao

import arrow.core.Either
import arrow.core.Option
import arrow.core.extensions.fx
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.SubmissionDoesNotExists
import com.annasystems.stoa.common.Version
import com.annasystems.stoa.common.doTry
import com.annasystems.stoa.common.serializers.InstantSerializer
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.dao.Rdbms.ABSTRACT
import com.annasystems.stoa.submission.dao.Rdbms.DESCRIPTION
import com.annasystems.stoa.submission.dao.Rdbms.EVENT_TIME
import com.annasystems.stoa.submission.dao.Rdbms.EVENT_TYPE
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSIONS
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_HISTORY
import com.annasystems.stoa.submission.dao.Rdbms.SUBMISSION_ID
import com.annasystems.stoa.submission.dao.Rdbms.TITLE
import com.annasystems.stoa.submission.dao.Rdbms.VERSION
import kotlinx.serialization.Serializable
import org.jooq.impl.DSL
import redis.clients.jedis.Jedis
import java.sql.Connection
import java.time.Instant
import java.time.ZoneOffset
import java.util.*


class SubmissionDao(
	private val getSubmissionFn: (SubmissionId) -> Either<ApplicationError, Submission>,
	private val getAllSubmissionsFn: () -> Either<ApplicationError, List<SubmissionRecord>>,
	private val getSubmissionHistoryFn: (SubmissionId) -> Either<ApplicationError, List<SubmissionHistory>>
) {

	fun getSubmission(submissionId: SubmissionId) = getSubmissionFn(submissionId)
	fun getSubmissions() = getAllSubmissionsFn()
	fun getSubmissionHistory(submissionId: SubmissionId) = getSubmissionHistoryFn(submissionId)

	companion object {
		@Serializable
		data class SubmissionRecord(
			val id: SubmissionId,
			val title: Title,
			val abstract: Abstract,
			val version: Version
		) {
			companion object {
				fun invoke(id: UUID, title: String, abstract: String, version: Int) = SubmissionRecord(
					id.toSubmissionId(),
					Title(title),
					Abstract(abstract),
					Version(version)
				)
			}
		}

		@Serializable
		data class SubmissionHistory(
			@Serializable(with = InstantSerializer::class) val timestamp: Instant,
			val type: String,
			val description: String
		)

		fun findSubmissionInRedis(jedis: () -> Jedis): (SubmissionId) -> Either<ApplicationError, Submission> = { submissionId ->
			Either.fx {
				val (submissionJson) = doTry { jedis().use { it.get(submissionId.asString()) } }
				val (submission) = Option.fromNullable(submissionJson).toEither { SubmissionDoesNotExists(submissionId) }
				val (parsedSubmissionJson) = submission.toSubmission()
				parsedSubmissionJson
			}
		}

		fun findSubmissionsInRdbms(connection: () -> Connection): () -> Either<ApplicationError, List<SubmissionRecord>> = {
			doTry {
				connection().use {
					DSL.using(it)
						.select(SUBMISSION_ID, TITLE, ABSTRACT, VERSION)
						.from(SUBMISSIONS)
						.fetch()
						.map { record ->
							SubmissionRecord.invoke(
								record[SUBMISSION_ID],
								record[TITLE],
								record[ABSTRACT],
								record[VERSION]
							)
						}
				}
			}
		}

		fun findSubmissionHistoryInRdbms(connection: () -> Connection): (SubmissionId) -> Either<ApplicationError, List<SubmissionHistory>> = { submissionId ->
			doTry {
				connection().use {
					DSL.using(it)
						.select(EVENT_TIME, EVENT_TYPE, DESCRIPTION)
						.from(SUBMISSION_HISTORY)
						.where(SUBMISSION_ID.eq(submissionId.raw))
						.fetch()
						.map { record ->
							SubmissionHistory(
								record[EVENT_TIME].toLocalDateTime().toInstant(ZoneOffset.UTC),
								record[EVENT_TYPE],
								record[DESCRIPTION]
							)
						}
				}
			}
		}
	}
}
