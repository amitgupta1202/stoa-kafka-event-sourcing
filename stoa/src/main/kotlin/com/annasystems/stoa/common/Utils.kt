package com.annasystems.stoa.common

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import java.util.*

object Utils : Logging {
	fun createTopics(bootstrapServers: String, allTopics: Set<Topic<out Any, out Any>>) {
		val props = Properties()
		props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
		val admin = AdminClient.create(props)

		val topicNames = allTopics.map { it.name }
		try {
			admin.deleteTopics(topicNames).all().get()
			Thread.sleep(1000)
		} catch (e: Exception) {
			logger().info("delete didn't happen as topics doesn't exists.")
		}

		val createTopic = allTopics.map { it.toNewTopic() }
		admin.createTopics(createTopic).all().get()

		logger().info("topics created successfully.")
		admin.close()
	}

	fun <K, V> createProducer(bootstrapServers: String, topicConfig: Topic<K, V>): Producer<K, V> {
		val props = Properties()
		props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
		props[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
		props[ProducerConfig.RETRIES_CONFIG] = 3 // this is now safe !!!!
		props[ProducerConfig.ACKS_CONFIG] = "all" // this has to be all
		props[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = 1 // this has to be 1
		return KafkaProducer<K, V>(props, topicConfig.keySerializer, topicConfig.valueSerializer)
	}

}