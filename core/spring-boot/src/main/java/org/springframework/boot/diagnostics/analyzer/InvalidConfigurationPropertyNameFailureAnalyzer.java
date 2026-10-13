/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyNameException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by
 * {@link InvalidConfigurationPropertyNameException}.
 *
 * @author Madhura Bhave
 */
class InvalidConfigurationPropertyNameFailureAnalyzer
		extends AbstractFailureAnalyzer<InvalidConfigurationPropertyNameException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationPropertyNameException cause) {
		BeanCreationException exception = findCause(rootFailure, BeanCreationException.class);
		String action = String.format("Modify '%s' so that it conforms to the canonical names requirements.",
				cause.getName());
		return new FailureAnalysis(buildDescription(cause, exception), action, cause);
	}

	private String buildDescription(InvalidConfigurationPropertyNameException cause,
			@Nullable BeanCreationException exception) {
		StringBuilder description = new StringBuilder(
				String.format("Configuration property name '%s' is not valid:%n", cause.getName()));
		String invalid = cause.getInvalidCharacters().stream().map(this::quote).collect(Collectors.joining(", "));
		description.append(String.format("%n    Invalid characters: %s", invalid));
		if (exception != null) {
			description.append(String.format("%n    Bean: %s", exception.getBeanName()));
		}
		description.append(String.format("%n    Reason: Canonical names should be "
				+ "kebab-case ('-' separated), lowercase alpha-numeric characters and must start with a letter"));
		return description.toString();
	}

	private String quote(Character c) {
		return "'" + c + "'";
	}

}
