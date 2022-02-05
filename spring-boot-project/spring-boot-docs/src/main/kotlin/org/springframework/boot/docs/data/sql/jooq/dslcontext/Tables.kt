/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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