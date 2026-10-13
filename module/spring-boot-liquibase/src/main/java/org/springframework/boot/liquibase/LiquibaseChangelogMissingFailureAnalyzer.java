/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase;

import liquibase.exception.ChangeLogParseException;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that analyzes exceptions of type
 * {@link ChangeLogParseException} caused by a Liquibase changelog not being present.
 *
 * @author Sebastiaan Fernandez
 */
class LiquibaseChangelogMissingFailureAnalyzer extends AbstractFailureAnalyzer<ChangeLogParseException> {

	private static final String MESSAGE_SUFFIX = " does not exist";

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, ChangeLogParseException cause) {
		String message = cause.getMessage();
		if (message != null && message.endsWith(MESSAGE_SUFFIX)) {
			String changelogPath = extractChangelogPath(message);
			return new FailureAnalysis(getDescription(changelogPath),
					"Make sure a Liquibase changelog is present at the configured path.", cause);
		}
		return null;
	}

	private String extractChangelogPath(String message) {
		return message.substring(0, message.length() - MESSAGE_SUFFIX.length());
	}

	private String getDescription(String changelogPath) {
		return "Liquibase failed to start because no changelog could be found at '" + changelogPath + "'.";
	}

}
