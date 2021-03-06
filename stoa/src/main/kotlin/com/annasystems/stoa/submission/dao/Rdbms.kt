package com.annasystems.stoa.submission.dao

import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType

object Rdbms {
	val SUBMISSIONS = DSL.table("SUBMISSIONS")!!
	val SUBMISSION_REVIEWERS = DSL.table("SUBMISSION_REVIEWERS")!!
	val SUBMISSION_EDITOR_INVITATIONS = DSL.table("SUBMISSION_EDITOR_INVITATIONS")!!
	val SUBMISSION_EDITOR_TASKS = DSL.table("SUBMISSION_EDITOR_TASKS")!!
	val SUBMISSION_HISTORY = DSL.table("SUBMISSION_HISTORY")!!

	val SUBMISSION_ID = DSL.field("SUBMISSION_ID", SQLDataType.UUID)!!
	val TITLE = DSL.field("TITLE", SQLDataType.VARCHAR(200))!!
	val ABSTRACT = DSL.field("ABSTRACT", SQLDataType.VARCHAR)!!
	val VERSION = DSL.field("VERSION", SQLDataType.INTEGER)!!
	val EDITOR_ID = DSL.field("EDITOR_ID", SQLDataType.UUID)!!
	val REVIEWER_ID = DSL.field("REVIEWER_ID", SQLDataType.UUID)!!
	val STATE = DSL.field("STATE", SQLDataType.VARCHAR(200))!!
	val CHASE_TIME = DSL.field("OVERDUE", SQLDataType.TIMESTAMP)!!

	val EVENT_NUMBER = DSL.field("EVENT_NUMBER", SQLDataType.INTEGER)!!
	val EVENT_TIME = DSL.field("EVENT_TIME", SQLDataType.TIMESTAMP)!!
	val EVENT_TYPE = DSL.field("EVENT_TYPE", SQLDataType.VARCHAR(100))!!
	val DESCRIPTION = DSL.field("DESCRIPTION", SQLDataType.VARCHAR(2000))!!
}