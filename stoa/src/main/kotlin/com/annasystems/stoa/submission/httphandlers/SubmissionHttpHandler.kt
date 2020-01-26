package com.annasystems.stoa.submission.httphandlers

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.getOrHandle
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.dao.SubmissionDao
import org.apache.kafka.clients.producer.Producer
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

fun submissionHttpHandler(submissionDao: SubmissionDao, submissionCommandName: String, submissionProducer: Producer<SubmissionId, SubmissionCommand>): HttpHandler {

	return routes(
		"/submissions" bind GET to { _: Request ->
			val okResponse: Either<ApplicationError, Response> = Either.fx {
				val (submissions) = submissionDao.getSubmissions()
				val (json) = submissions.toSubmissionRecordJson()
				Response(OK).body(json)
			}
			okResponse.getOrHandle { Response(INTERNAL_SERVER_ERROR).body(it.message()) }
		},

		"/submissions/{submissionId}" bind GET to { request: Request ->
			val okResponse: Either<ApplicationError, Response> = Either.fx {
				val (submissionId) = request.path("submissionId").toSubmissionId()
				val (submission) = submissionDao.getSubmission(submissionId)
				val (json) = submission.toJson()
				Response(OK).body(json)
			}
			okResponse.getOrHandle { Response(NOT_FOUND).body(it.message()) }
		},

		"/submissions/{submissionId}/history" bind GET to { request: Request ->
			val okResponse: Either<ApplicationError, Response> = Either.fx {
				val (submissionId) = request.path("submissionId").toSubmissionId()
				val (submissionHistory) = submissionDao.getSubmissionHistory(submissionId)
				val (json) = submissionHistory.toSubmissionHistoryJson()
				Response(OK).body(json)
			}
			okResponse.getOrHandle { Response(NOT_FOUND).body(it.message()) } //TODO: map ApplicationError to HttpResponseCode
		},

		"submissions/async" bind POST to { request: Request ->
			val okResponse: Either<ApplicationError, Response> = Either.fx {
				val (command) = request.bodyString().getSubmissionCommand()
				val (sent) = submissionProducer.sendCommand(submissionCommandName, command)
				Response(OK).body(sent.offset().toString())
			}
			okResponse.getOrHandle { Response(INTERNAL_SERVER_ERROR).body(it.message()) }
		}
	)
}





