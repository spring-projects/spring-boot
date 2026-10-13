/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.Assert;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by any
 * {@link UnboundConfigurationPropertiesException}.
 *
 * @author Madhura Bhave
 */
class UnboundConfigurationPropertyFailureAnalyzer
		extends AbstractFailureAnalyzer<UnboundConfigurationPropertiesException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, UnboundConfigurationPropertiesException cause) {
		BindException exception = findCause(rootFailure, BindException.class);
		Assert.state(exception != null, "BindException not found");
		return analyzeUnboundConfigurationPropertiesException(exception, cause);
	}

	private FailureAnalysis analyzeUnboundConfigurationPropertiesException(BindException cause,
			UnboundConfigurationPropertiesException exception) {
		StringBuilder description = new StringBuilder(
				String.format("Binding to target %s failed:%n", cause.getTarget()));
		for (ConfigurationProperty property : exception.getUnboundProperties()) {
			buildDescription(description, property);
			description.append(String.format("%n    Reason: %s", exception.getMessage()));
		}
		return getFailureAnalysis(description, cause);
	}

	private void buildDescription(StringBuilder description, @Nullable ConfigurationProperty property) {
		if (property != null) {
			description.append(String.format("%n    Property: %s", property.getName()));
			description.append(String.format("%n    Value: \"%s\"", property.getValue()));
			description.append(String.format("%n    Origin: %s", property.getOrigin()));
		}
	}

	private FailureAnalysis getFailureAnalysis(Object description, BindException cause) {
		return new FailureAnalysis(description.toString(), "Update your application's configuration", cause);
	}

}
