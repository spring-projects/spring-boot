/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jooq;

import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultExecuteListener;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/**
 * Transforms {@link java.sql.SQLException} into a Spring-specific
 * {@link DataAccessException}.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class JooqExceptionTranslator extends DefaultExecuteListener {

	// Based on the jOOQ-spring-example from https://github.com/jOOQ/jOOQ

	private static final Log logger = LogFactory.getLog(JooqExceptionTranslator.class);

	@Override
	public void exception(ExecuteContext context) {
		SQLExceptionTranslator translator = getTranslator(context);
		// The exception() callback is not only triggered for SQL exceptions but also for
		// "normal" exceptions. In those cases sqlException() returns null.
		SQLException exception = context.sqlException();
		while (exception != null) {
			handle(context, translator, exception);
			exception = exception.getNextException();
		}
	}

	private SQLExceptionTranslator getTranslator(ExecuteContext context) {
		SQLDialect dialect = context.configuration().dialect();
		if (dialect != null && dialect.thirdParty() != null) {
			String dbName = dialect.thirdParty().springDbName();
			if (dbName != null) {
				return new SQLErrorCodeSQLExceptionTranslator(dbName);
			}
		}
		return new SQLStateSQLExceptionTranslator();
	}

	/**
	 * Handle a single exception in the chain. SQLExceptions might be nested multiple
	 * levels deep. The outermost exception is usually the least interesting one ("Call
	 * getNextException to see the cause."). Therefore the innermost exception is
	 * propagated and all other exceptions are logged.
	 * @param context the execute context
	 * @param translator the exception translator
	 * @param exception the exception
	 */
	private void handle(ExecuteContext context, SQLExceptionTranslator translator,
			SQLException exception) {
		DataAccessException translated = translate(context, translator, exception);
		if (exception.getNextException() == null) {
			context.exception(translated);
		}
		else {
			logger.error("Execution of SQL statement failed.", translated);
		}
	}

	private DataAccessException translate(ExecuteContext context,
			SQLExceptionTranslator translator, SQLException exception) {
		return translator.translate("jOOQ", context.sql(), exception);
	}

}
