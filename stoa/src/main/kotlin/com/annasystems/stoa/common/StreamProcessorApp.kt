package com.annasystems.stoa.common

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import java.util.*
import java.util.concurrent.CountDownLatch

abstract class StreamProcessorApp : Logging {
	abstract val appId: String
	abstract val topology: Topology
	abstract val bootstrapServers: String

	private val latch = CountDownLatch(1)
	private val streams: KafkaStreams
		get() {
			val props = Properties()
			props[StreamsConfig.APPLICATION_ID_CONFIG] = appId
			props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
			props[StreamsConfig.PROCESSING_GUARANTEE_CONFIG] = StreamsConfig.EXACTLY_ONCE
			return KafkaStreams(topology, props)
		}

	fun start() {
		logger().info("Starting $appId...")
		streams.setStateListener { newState, oldState -> logger().info(("$oldState -> $newState")) }
		streams.setUncaughtExceptionHandler { _: Thread?, _: Throwable? ->
			logger().info("Exception was thrown in stream processor $appId")
			latch.countDown()
		}
		streams.start()
		Runtime.getRuntime().addShutdownHook(object : Thread() {
			override fun run() {
				close()
			}
		})
		latch.await()
	}

	open fun close() {
		logger().info("Shutting down $appId...")
		streams.close()
	}
}