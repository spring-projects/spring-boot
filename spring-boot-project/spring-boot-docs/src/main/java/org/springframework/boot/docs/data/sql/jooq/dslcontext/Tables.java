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

package org.springframework.boot.docs.data.sql.jooq.dslcontext;

import java.util.GregorianCalendar;

import org.jooq.Name;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.TableImpl;
import org.jooq.impl.TableRecordImpl;

/**
 * Tables class.
 */
abstract class Tables {

	static final TAuthor AUTHOR = null;

	/**
	 * TAuthor class.
	 */
	abstract class TAuthor extends TableImpl<TAuthorRecord> {

		/**
		 * Constructs a new TAuthor object with the specified name.
		 * @param name the name of the author
		 */
		TAuthor(Name name) {
			super(name);
		}

		public final TableField<TAuthorRecord, GregorianCalendar> DATE_OF_BIRTH = null;

	}

	/**
	 * TAuthorRecord class.
	 */
	abstract class TAuthorRecord extends TableRecordImpl<TAuthorRecord> {

		/**
		 * Constructs a new TAuthorRecord object with the specified table.
		 * @param table the table to be used for constructing the TAuthorRecord object
		 */
		TAuthorRecord(Table<TAuthorRecord> table) {
			super(table);
		}

	}

}
