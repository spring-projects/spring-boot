/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import java.sql.SQLException;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.ExecuteContext;
import org.jooq.SQLDialect;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ExceptionTranslatorExecuteListener} that delegates to
 * an {@link SQLExceptionTranslator}.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
final class DefaultExceptionTranslatorExecuteListener implements ExceptionTranslatorExecuteListener {

	// Based on the jOOQ-spring-example from https://github.com/jOOQ/jOOQ

	private static final Log defaultLogger = LogFactory.getLog(ExceptionTranslatorExecuteListener.class);

	private final Log logger;

	private Function<ExecuteContext, SQLExceptionTranslator> translatorFactory;

	/**
     * Constructs a new DefaultExceptionTranslatorExecuteListener with the specified default logger and translator factory.
     * 
     * @param defaultLogger the default logger to be used by the listener
     * @param translatorFactory the translator factory to be used by the listener
     */
    DefaultExceptionTranslatorExecuteListener() {
		this(defaultLogger, new DefaultTranslatorFactory());
	}

	/**
     * Constructs a new DefaultExceptionTranslatorExecuteListener with the specified translatorFactory.
     *
     * @param translatorFactory the factory used to create the SQLExceptionTranslator
     */
    DefaultExceptionTranslatorExecuteListener(Function<ExecuteContext, SQLExceptionTranslator> translatorFactory) {
		this(defaultLogger, translatorFactory);
	}

	/**
     * Constructs a new DefaultExceptionTranslatorExecuteListener with the specified logger and a default translator factory.
     * 
     * @param logger the logger to be used for logging
     */
    DefaultExceptionTranslatorExecuteListener(Log logger) {
		this(logger, new DefaultTranslatorFactory());
	}

	/**
     * Constructs a new DefaultExceptionTranslatorExecuteListener with the specified logger and translator factory.
     * 
     * @param logger the logger to be used for logging exceptions
     * @param translatorFactory the factory function used to create SQLExceptionTranslators
     * @throws IllegalArgumentException if the translatorFactory is null
     */
    private DefaultExceptionTranslatorExecuteListener(Log logger,
			Function<ExecuteContext, SQLExceptionTranslator> translatorFactory) {
		Assert.notNull(translatorFactory, "TranslatorFactory must not be null");
		this.logger = logger;
		this.translatorFactory = translatorFactory;
	}

	/**
     * This method is called when an exception occurs during the execution of a SQL statement.
     * It handles the exception by translating it using the provided translator and then
     * recursively handles any chained exceptions.
     * 
     * @param context The execution context containing information about the exception.
     */
    @Override
	public void exception(ExecuteContext context) {
		SQLExceptionTranslator translator = this.translatorFactory.apply(context);
		// The exception() callback is not only triggered for SQL exceptions but also for
		// "normal" exceptions. In those cases sqlException() returns null.
		SQLException exception = context.sqlException();
		while (exception != null) {
			handle(context, translator, exception);
			exception = exception.getNextException();
		}
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
	private void handle(ExecuteContext context, SQLExceptionTranslator translator, SQLException exception) {
		DataAccessException translated = translator.translate("jOOQ", context.sql(), exception);
		if (exception.getNextException() != null) {
			this.logger.error("Execution of SQL statement failed.", (translated != null) ? translated : exception);
			return;
		}
		if (translated != null) {
			context.exception(translated);
		}
	}

	/**
	 * Default {@link SQLExceptionTranslator} factory that creates the translator based on
	 * the Spring DB name.
	 */
	private static final class DefaultTranslatorFactory implements Function<ExecuteContext, SQLExceptionTranslator> {

		/**
         * Applies the SQL exception translator to the given execute context.
         * 
         * @param context the execute context
         * @return the SQL exception translator
         */
        @Override
		public SQLExceptionTranslator apply(ExecuteContext context) {
			return apply(context.configuration().dialect());
		}

		/**
         * Applies the appropriate SQLExceptionTranslator based on the given SQLDialect.
         * 
         * @param dialect the SQLDialect to determine the appropriate SQLExceptionTranslator
         * @return the SQLExceptionTranslator to be used
         */
        private SQLExceptionTranslator apply(SQLDialect dialect) {
			String dbName = getSpringDbName(dialect);
			return (dbName != null) ? new SQLErrorCodeSQLExceptionTranslator(dbName)
					: new SQLStateSQLExceptionTranslator();
		}

		/**
         * Returns the Spring database name for the given SQL dialect.
         * 
         * @param dialect the SQL dialect
         * @return the Spring database name, or null if the dialect is null or does not have a third party implementation
         */
        private String getSpringDbName(SQLDialect dialect) {
			return (dialect != null && dialect.thirdParty() != null) ? dialect.thirdParty().springDbName() : null;
		}

	}

}
