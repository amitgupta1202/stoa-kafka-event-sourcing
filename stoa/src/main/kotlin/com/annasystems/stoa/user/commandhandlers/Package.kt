package com.annasystems.stoa.user.commandhandlers

import com.annasystems.stoa.common.Version
import com.annasystems.stoa.user.UserEvent
import com.annasystems.stoa.user.UserCommand.Companion.Metadata as CommandMetadata

internal fun CommandMetadata.toEventMetadata(version: Version) = UserEvent.Companion.Metadata(requestId, actorId, timestamp, userId, version.nextVersion())
