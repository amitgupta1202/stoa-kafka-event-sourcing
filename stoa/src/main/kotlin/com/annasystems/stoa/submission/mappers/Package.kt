package com.annasystems.stoa.submission.mappers

import com.annasystems.stoa.common.Version
import com.annasystems.stoa.submission.SubmissionCommand
import com.annasystems.stoa.submission.SubmissionEvent

internal fun SubmissionEvent.Companion.Metadata.toCommandMetadata(expectedVersion: Version = Version.ANY): SubmissionCommand.Companion.Metadata =
	with(this) { SubmissionCommand.Companion.Metadata(requestId, actorId, timestamp, submissionId, expectedVersion) } //TODO fix timestamp, use clock