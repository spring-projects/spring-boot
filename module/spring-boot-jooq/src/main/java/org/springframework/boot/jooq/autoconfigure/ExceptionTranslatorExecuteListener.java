/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

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
 * @since 4.0.0
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
