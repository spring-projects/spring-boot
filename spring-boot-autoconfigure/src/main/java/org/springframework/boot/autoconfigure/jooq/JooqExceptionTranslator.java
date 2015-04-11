/*
 * Copyright 2012-2015 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultExecuteListener;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.AbstractFallbackSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import java.sql.SQLException;

/**
 * Transforms {@link java.sql.SQLException} into a Spring-specific @{link
 * DataAccessException}.
 *
 * @author Petri Kainulainen
 * @author Adam Zell
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @see <a
 * href="https://github.com/jOOQ/jOOQ/commit/03b4797a4548ff8e46fe3368e719ae0d14a9cd90#diff-031df6070c4380b5b04bbd7f83ff01cd">Original
 * source</a>
 */
class JooqExceptionTranslator extends DefaultExecuteListener {

	private static final Log logger = LogFactory.getLog(JooqExceptionTranslator.class);

	@Override
	public void exception(ExecuteContext ctx) {
		SQLDialect dialect = ctx.configuration().dialect();

		AbstractFallbackSQLExceptionTranslator translator;
		if (dialect != null) {
			translator = new SQLErrorCodeSQLExceptionTranslator(dialect.name());
		}
		else {
			translator = new SQLStateSQLExceptionTranslator();
		}

		// The exception() callback is not only triggered for SQL exceptions but also for "normal" exceptions. In those
		// cases sqlException() returns null.
		SQLException sqlException = ctx.sqlException();

		// SQLExceptions might be nested multiple levels deep. The outermost exception is usually the least interesting
		// one ("Call getNextException to see the cause."). Therefore the innermost exception is propagated and all
		// other exceptions are logged.
		while (sqlException != null) {
			DataAccessException translatedException = translator.translate("jOOQ", ctx.sql(), sqlException);
			ctx.exception(translatedException);
			sqlException = sqlException.getNextException();

			// Do not log the exception if it's the innermost. Otherwise it might be displayed twice, once in the log
			// and once as exception.
			if (sqlException != null) {
				logger.error("Execution of SQL statement failed.", translatedException);
			}
		}
	}
}
