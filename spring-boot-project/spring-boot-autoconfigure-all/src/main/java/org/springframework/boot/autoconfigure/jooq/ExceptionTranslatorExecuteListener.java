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

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLExceptionTranslator;

/**
 * An {@link ExecuteListener} used by the auto-configured
 * {@link DefaultExecuteListenerProvider} to translate exceptions in the
 * {@link ExecuteContext}. Most commonly used to translate {@link SQLException
 * SQLExceptions} to Spring-specific {@link DataAccessException DataAccessExceptions} by
 * adapting an existing {@link SQLExceptionTranslator}.
 *
 * @author Dennis Melzer
 * @since 3.3.0
 * @see #DEFAULT
 * @see #of(Function)
 */
public interface ExceptionTranslatorExecuteListener extends ExecuteListener {

	/**
	 * Default {@link ExceptionTranslatorExecuteListener} suitable for most applications.
	 */
	ExceptionTranslatorExecuteListener DEFAULT = new DefaultExceptionTranslatorExecuteListener();

	/**
	 * Creates a new {@link ExceptionTranslatorExecuteListener} backed by an
	 * {@link SQLExceptionTranslator}.
	 * @param translatorFactory factory function used to create the
	 * {@link SQLExceptionTranslator}
	 * @return a new {@link ExceptionTranslatorExecuteListener} instance
	 */
	static ExceptionTranslatorExecuteListener of(Function<ExecuteContext, SQLExceptionTranslator> translatorFactory) {
		return new DefaultExceptionTranslatorExecuteListener(translatorFactory);
	}

}
