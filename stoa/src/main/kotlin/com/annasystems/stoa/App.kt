package com.annasystems.stoa

import com.annasystems.stoa.common.Configs
import com.annasystems.stoa.common.Utils
import com.annasystems.stoa.submission.dao.SubmissionDao
import com.annasystems.stoa.submission.httphandlers.submissionHttpHandler
import com.annasystems.stoa.submission.mappers.ToSubmissionHistoryTypeAndDescription
import com.annasystems.stoa.submission.processors.*
import com.annasystems.stoa.user.dao.UserDao
import com.annasystems.stoa.user.dao.UserDao.Companion.findUserInRedis
import com.annasystems.stoa.user.processors.UserCommandHandlerProcessor
import com.annasystems.stoa.user.processors.UserProjectorProcessor
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

object App {

	fun start() {
		val executor = Executors.newFixedThreadPool(9)
		val jedisPool = Configs.Redis.jedisPool
		val dataSource = Configs.Postgres.dataSource

		val userDao = UserDao(findUserInRedis { jedisPool.resource })
		val submissionDao = SubmissionDao(
			SubmissionDao.findSubmissionInRedis { jedisPool.resource },
			SubmissionDao.findSubmissionsInRdbms { dataSource.connection },
			SubmissionDao.findSubmissionHistoryInRdbms { dataSource.connection }
		)

		val userProjectorProcessor = UserProjectorProcessor(
			Configs.bootstrapServers,
			Configs.Topics.userModels,
			Configs.Serialization.json
		) { jedisPool.resource }
		val userCommandHandlerProcessor = UserCommandHandlerProcessor(
			Configs.bootstrapServers,
			Configs.Topics.userCommands,
			Configs.Topics.userEvents,
			Configs.Topics.userModels,
			Configs.Topics.userCommandResults,
			Configs.KeyValueStores.users
		)

		val submissionCommandHandlerProcessor = SubmissionCommandHandlerProcessor(
			Configs.bootstrapServers,
			Configs.Topics.submissionCommands,
			Configs.Topics.submissionEvents,
			Configs.Topics.submissionModels,
			Configs.Topics.submissionCommandResults,
			Configs.KeyValueStores.submissions,
			userDao
		)
		val submissionProjectorProcessor = SubmissionProjectorProcessor(
			Configs.bootstrapServers,
			Configs.Topics.submissionModels,
			Configs.Serialization.json
		) { jedisPool.resource }
		val submissionEmailEventProcessor = SubmissionEmailEventProcessor(
			Configs.bootstrapServers,
			Configs.Topics.submissionEvents,
			Configs.Topics.submissionEmails
		)
		val submissionScheduleChaserProcessor = SubmissionScheduleChaserProcessor(
			Configs.bootstrapServers,
			Configs.Topics.submissionEvents
		) { jedisPool.resource }
		val submissionSendChaserProcessor = SubmissionSendChaserProcessor(
			Configs.bootstrapServers,
			Configs.Topics.submissionChaseTrigger,
			Configs.Topics.submissionCommands
		) { jedisPool.resource }
		val submissionListProjectorProcessor = SubmissionListProjectorProcessor(
			Configs.bootstrapServers,
			Configs.Topics.submissionModels
		) { dataSource.connection }
		val submissionHistoryProjectorProcessor = SubmissionHistoryProjectorProcessor(
			Configs.bootstrapServers,
			Configs.Topics.submissionEvents,
			ToSubmissionHistoryTypeAndDescription(userDao)
		) { dataSource.connection }

		executor.submit { userProjectorProcessor.start() }
		executor.submit { userCommandHandlerProcessor.start() }
		executor.submit { submissionProjectorProcessor.start() }
		executor.submit { submissionCommandHandlerProcessor.start() }
		executor.submit { submissionEmailEventProcessor.start() }
		executor.submit { submissionScheduleChaserProcessor.start() }
		executor.submit { submissionSendChaserProcessor.start() }
		executor.submit { submissionListProjectorProcessor.start() }
		executor.submit { submissionHistoryProjectorProcessor.start() }

		submissionHttpHandler(submissionDao, Configs.Topics.submissionCommands.name, Utils.createProducer(Configs.bootstrapServers, Configs.Topics.submissionCommands)).asServer(Netty(9000)).start()
	}
}

fun main() {
	val latch = CountDownLatch(1)
	Utils.createTopics(Configs.bootstrapServers, Configs.Topics.all)
	App.start()
	latch.await()
}
