package com.annasystems.stoa.user.processors

import arrow.core.Option
import com.annasystems.stoa.common.*
import com.annasystems.stoa.common.serdes.JsonSerde
import com.annasystems.stoa.user.*
import com.annasystems.stoa.user.commandhandlers.CreateUserCommandHandler
import com.annasystems.stoa.user.processors.UserCommandTransformer.Companion.UserCommandResult
import com.annasystems.stoa.user.serdes.UserIdSerde
import kotlinx.serialization.Serializable
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores

class UserCommandHandlerProcessor(
	override val bootstrapServers: String,
	private val userCommandTopic: Topic<UserId, UserCommand>,
	private val userEventTopic: Topic<UserId, UserEvent>,
	private val userModelTopic: Topic<UserId, User>,
	private val userCommandResultTopic: Topic<UserId, UserCommandResult>,
	private val userStoreName: String
) : StreamProcessorApp() {
	override val appId: String
		get() = "processor-user-command-handler"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val usersStoreBuilder = Stores.keyValueStoreBuilder(
				Stores.persistentKeyValueStore(userStoreName),
				UserIdSerde,
				JsonSerde(User.serializer())
			)

			builder.addStateStore(usersStoreBuilder)

			val userCommands =
				with(userCommandTopic) {
					builder.stream(name, Consumed.with(keySerde, valueSerde))
				}

			val results = userCommands.transform(
				TransformerSupplier<UserId, UserCommand, KeyValue<UserId, UserCommandResult>> { UserCommandTransformer(userStoreName) },
				userStoreName
			)

			val successfulResults = results.flatMapValues<UserCommandResult.Success>(ToSuccessful)
			val users = successfulResults.mapValues<User>(ToUser)
			val userEvents = successfulResults.mapValues<UserEvent>(ToUserEvents)

			with(userCommandResultTopic) {
				results.to(name, Produced.with(keySerde, valueSerde))
			}

			with(userModelTopic) {
				users.to(name, Produced.with(keySerde, valueSerde))
			}

			with(userEventTopic) {
				userEvents.to(name, Produced.with(keySerde, valueSerde))
			}

			return builder.build()
		}


	object ToSuccessful : ValueMapper<UserCommandResult, Iterable<UserCommandResult.Success>> {
		override fun apply(value: UserCommandResult?): Iterable<UserCommandResult.Success> {
			checkNotNull(value)
			return when (value) {
				is UserCommandResult.Success -> listOf(value)
				is UserCommandResult.Failure -> emptyList()
			}
		}
	}

	object ToUser : ValueMapper<UserCommandResult.Success, User> {
		override fun apply(value: UserCommandResult.Success?): User {
			checkNotNull(value)
			return value.outcome.model
		}
	}

	object ToUserEvents : ValueMapper<UserCommandResult.Success, UserEvent> {
		override fun apply(value: UserCommandResult.Success?): UserEvent {
			checkNotNull(value)
			return value.outcome.event
		}
	}
}

class UserCommandTransformer(private val userStoreName: String) : Transformer<UserId, UserCommand, KeyValue<UserId, UserCommandResult>> {

	private lateinit var context: ProcessorContext
	private lateinit var userStore: KeyValueStore<UserId, User>
	private lateinit var userCommandHandler: UserCommandHandler

	override fun init(processorContext: ProcessorContext?) {
		checkNotNull(processorContext)
		context = processorContext
		@Suppress("UNCHECKED_CAST")
		userStore = processorContext.getStateStore(userStoreName) as KeyValueStore<UserId, User>
		val getUser: GetUser = {
			Option.fromNullable(userStore.get(it)).toEither { UserDoesNotExists(it) }
		}
		userCommandHandler = UserCommandHandler(
			createUserCommandHandler = CreateUserCommandHandler(),
			getUser = getUser
		)
	}

	override fun transform(userId: UserId?, command: UserCommand?): KeyValue<UserId, UserCommandResult> {
		checkNotNull(userId)
		checkNotNull(command)
		val commandResult = userCommandHandler.handle(command)
		commandResult.map {
			userStore.put(userId, it.model)
		}
		val result = commandResult.fold({ UserCommandResult.Failure(it) }, { UserCommandResult.Success(it) })
		return KeyValue.pair(userId, result)
	}

	override fun close() {}

	companion object {
		@Serializable
		sealed class UserCommandResult {
			@Serializable
			data class Success(val outcome: CommandResult<UserEvent, User>) : UserCommandResult()

			@Serializable
			data class Failure(val error: ApplicationError) : UserCommandResult()
		}
	}
}