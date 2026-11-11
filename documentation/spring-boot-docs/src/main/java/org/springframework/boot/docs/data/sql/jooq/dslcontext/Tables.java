/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.jooq.dslcontext;

import java.util.GregorianCalendar;

import org.jooq.Name;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.TableImpl;
import org.jooq.impl.TableRecordImpl;

abstract class Tables {

	static final TAuthor AUTHOR = null;

	abstract class TAuthor extends TableImpl<TAuthorRecord> {

		TAuthor(Name name) {
			super(name);
		}

		public final TableField<TAuthorRecord, GregorianCalendar> DATE_OF_BIRTH = null;

	}

	abstract class TAuthorRecord extends TableRecordImpl<TAuthorRecord> {

		TAuthorRecord(Table<TAuthorRecord> table) {
			super(table);
		}

	}

}
