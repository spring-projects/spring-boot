/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of a Hikari configuration
 * failure caused by the use of the unsupported 'dataSourceClassName' property.
 *
 * @author Stephane Nicoll
 */
class HikariDriverConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<CannotGetJdbcConnectionException> {

	private static final String EXPECTED_MESSAGE = "cannot use driverClassName and dataSourceClassName together.";

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, CannotGetJdbcConnectionException cause) {
		Throwable subCause = cause.getCause();
		if (subCause == null || !EXPECTED_MESSAGE.equals(subCause.getMessage())) {
			return null;
		}
		return new FailureAnalysis(
				"Configuration of the Hikari connection pool failed: 'dataSourceClassName' is not supported.",
				"Spring Boot auto-configures only a driver and can't specify a custom "
						+ "DataSource. Consider configuring the Hikari DataSource in your own configuration.",
				cause);
	}

}
