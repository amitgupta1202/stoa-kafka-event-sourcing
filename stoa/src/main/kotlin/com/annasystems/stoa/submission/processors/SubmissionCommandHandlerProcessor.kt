package com.annasystems.stoa.submission.processors

import arrow.core.Option
import com.annasystems.stoa.common.ApplicationError
import com.annasystems.stoa.common.StreamProcessorApp
import com.annasystems.stoa.common.SubmissionDoesNotExists
import com.annasystems.stoa.common.Topic
import com.annasystems.stoa.common.serdes.JsonSerde
import com.annasystems.stoa.submission.*
import com.annasystems.stoa.submission.commandhandlers.*
import com.annasystems.stoa.submission.processors.SubmissionCommandTransformer.Companion.SubmissionCommandResult
import com.annasystems.stoa.submission.serdes.SubmissionIdSerde
import com.annasystems.stoa.user.GetAuthor
import com.annasystems.stoa.user.GetEditor
import com.annasystems.stoa.user.GetReviewer
import com.annasystems.stoa.user.GetUser
import com.annasystems.stoa.user.dao.UserDao
import kotlinx.serialization.Serializable
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores

class SubmissionCommandHandlerProcessor(
	override val bootstrapServers: String,
	private val submissionCommandTopic: Topic<SubmissionId, SubmissionCommand>,
	private val submissionEventTopic: Topic<SubmissionId, SubmissionEvent>,
	private val submissionModelTopic: Topic<SubmissionId, Submission>,
	private val submissionCommandResultTopic: Topic<SubmissionId, SubmissionCommandResult>,
	private val submissionStoreName: String,
	private val userDao: UserDao
) : StreamProcessorApp() {
	override val appId: String
		get() = "processor-submission-command-handler"

	override val topology: Topology
		get() {
			val builder = StreamsBuilder()

			val submissionsStoreBuilder = Stores.keyValueStoreBuilder(
				Stores.persistentKeyValueStore(submissionStoreName),
				SubmissionIdSerde,
				JsonSerde(Submission.serializer())
			)
			builder.addStateStore(submissionsStoreBuilder)

			val submissionCommands = with(submissionCommandTopic) { builder.stream(name, Consumed.with(keySerde, valueSerde)) }

			val results =
				submissionCommands.transform(TransformerSupplier { SubmissionCommandTransformer(submissionStoreName, userDao) }, submissionStoreName)

			val successfulResults = results.flatMapValues(ToSuccessful)
			val submissions = successfulResults.mapValues(ToSubmission)
			val submissionEvents = successfulResults.flatMapValues(ToSubmissionEvents)

			with(submissionEventTopic) { submissionEvents.to(name, Produced.with(keySerde, valueSerde)) }
			with(submissionModelTopic) { submissions.to(name, Produced.with(keySerde, valueSerde)) }
			with(submissionCommandResultTopic) { results.to(name, Produced.with(keySerde, valueSerde)) }

			return builder.build()
		}

	companion object {
		object ToSuccessful : ValueMapper<SubmissionCommandResult, Iterable<SubmissionCommandResult.Success>> {
			override fun apply(value: SubmissionCommandResult?): Iterable<SubmissionCommandResult.Success> {
				checkNotNull(value)
				return when (value) {
					is SubmissionCommandResult.Success -> listOf(value)
					is SubmissionCommandResult.Failure -> emptyList()
				}
			}
		}

		object ToSubmission : ValueMapper<SubmissionCommandResult.Success, Submission> {
			override fun apply(value: SubmissionCommandResult.Success?): Submission {
				checkNotNull(value)
				return value.submission
			}
		}

		object ToSubmissionEvents : ValueMapper<SubmissionCommandResult.Success, Iterable<SubmissionEvent>> {
			override fun apply(value: SubmissionCommandResult.Success?): Iterable<SubmissionEvent> {
				checkNotNull(value)
				return value.events
			}
		}
	}
}

@Suppress("DuplicatedCode")
class SubmissionCommandTransformer(private val submissionStoreName: String, private val userDao: UserDao) : Transformer<SubmissionId, SubmissionCommand, KeyValue<SubmissionId, SubmissionCommandResult>> {

	private lateinit var context: ProcessorContext
	private lateinit var submissionStore: KeyValueStore<SubmissionId, Submission>
	private lateinit var submissionCommandHandler: SubmissionCommandHandler

	override fun init(processorContext: ProcessorContext?) {
		checkNotNull(processorContext)
		context = processorContext
		@Suppress("UNCHECKED_CAST")
		submissionStore = processorContext.getStateStore(submissionStoreName) as KeyValueStore<SubmissionId, Submission>

		val getSubmission: GetSubmission = { Option.fromNullable(submissionStore.get(it)).toEither {
			SubmissionDoesNotExists(
				it
			)
		} }
		val getAuthor: GetAuthor = { userDao.getAuthor(it) }
		val getEditor: GetEditor = { userDao.getEditor(it) }
		val getReviewer: GetReviewer = { userDao.getReviewer(it) }
		val getUser: GetUser = { userDao.getUser(it) }

		submissionCommandHandler = SubmissionCommandHandler(
			createSubmissionCommandHandler = CreateSubmissionCommandHandler(getAuthor),
			assignEditorCommandHandler = AssignEditorCommandHandler(getEditor),
			unassignEditorCommandHandler = UnassignEditorCommandHandler(getEditor),
			addReviewerCommandHandler = AddReviewerCommandHandler(getReviewer),
			removeReviewerCommandHandler = RemoveReviewerCommandHandler(getReviewer),
			inviteEditorCommandHandler = InviteEditorCommandHandler(getEditor),
			respondToEditorInvitationCommandHandler = RespondToEditorInvitationCommandHandler(getEditor),
			sendInviteEditorEmailCommandHandler = SendInviteEditorEmailCommandHandler(getEditor),
			createEditorTaskToAddReviewerCommandHandler = CreateEditorTaskToAddReviewerCommandHandler(getEditor),
			changeEditorTaskToAddReviewerCommandHandler = ChangeEditorTaskToAddReviewerCommandHandler(getEditor),
			sendEditorChaseToAddReviewerEmailCommandHandler = SendEditorChaseToAddReviewerEmailCommandHandler(getEditor),
			getUser = getUser,
			getSubmission = getSubmission
		)
	}

	override fun transform(submissionId: SubmissionId?, command: SubmissionCommand?): KeyValue<SubmissionId, SubmissionCommandResult> {
		checkNotNull(submissionId)
		checkNotNull(command)
		val commandResult = submissionCommandHandler.handle(command)
		commandResult.map { submissionStore.put(submissionId, it.second) }
		val result = commandResult.fold({ SubmissionCommandResult.Failure(it) }, { SubmissionCommandResult.Success(it.first, it.second) })
		return KeyValue.pair(submissionId, result)
	}

	override fun close() {}

	companion object {
		@Serializable
		sealed class SubmissionCommandResult {
			@Serializable
			data class Success(val events: List<SubmissionEvent>, val submission: Submission) : SubmissionCommandResult()

			@Serializable
			data class Failure(val error: ApplicationError) : SubmissionCommandResult()
		}
	}
}