/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.jooq.dslcontext

import org.jooq.Name
import org.jooq.Table
import org.jooq.TableField
import org.jooq.impl.TableImpl
import org.jooq.impl.TableRecordImpl
import java.util.GregorianCalendar

object Tables {

	val AUTHOR: TAuthor? = null

	abstract class TAuthor(name: Name?) : TableImpl<TAuthorRecord?>(name) {
		val DATE_OF_BIRTH: TableField<TAuthorRecord, GregorianCalendar>? = null
	}

	abstract class TAuthorRecord(table: Table<TAuthorRecord?>?) : TableRecordImpl<TAuthorRecord?>(table)

}

