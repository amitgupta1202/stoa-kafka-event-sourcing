package com.annasystems.stoa.submission.commandhandlers

import com.annasystems.stoa.common.Version
import com.annasystems.stoa.submission.SubmissionEvent
import com.annasystems.stoa.submission.SubmissionCommand.Companion.Metadata as CommandMetadata

internal fun CommandMetadata.toEventMetadata(version: Version) = SubmissionEvent.Companion.Metadata(requestId, actorId, timestamp, submissionId, version.nextVersion())
