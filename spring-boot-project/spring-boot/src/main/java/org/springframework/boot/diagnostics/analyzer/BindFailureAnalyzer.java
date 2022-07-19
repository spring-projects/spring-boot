/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BindException} excluding {@link BindValidationException} and
 * {@link UnboundConfigurationPropertiesException}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class BindFailureAnalyzer extends AbstractFailureAnalyzer<BindException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, BindException cause) {
		Throwable rootCause = cause.getCause();
		if (rootCause instanceof BindValidationException
				|| rootCause instanceof UnboundConfigurationPropertiesException) {
			return null;
		}
		return analyzeGenericBindException(cause);
	}

	private FailureAnalysis analyzeGenericBindException(BindException cause) {
		StringBuilder description = new StringBuilder(String.format("%s:%n", cause.getMessage()));
		ConfigurationProperty property = cause.getProperty();
		buildDescription(description, property);
		description.append(String.format("%n    Reason: %s", getMessage(cause)));
		return getFailureAnalysis(description, cause);
	}

	private void buildDescription(StringBuilder description, ConfigurationProperty property) {
		if (property != null) {
			description.append(String.format("%n    Property: %s", property.getName()));
			description.append(String.format("%n    Value: \"%s\"", property.getValue()));
			description.append(String.format("%n    Origin: %s", property.getOrigin()));
		}
	}

	private String getMessage(BindException cause) {
		Throwable rootCause = getRootCause(cause.getCause());
		ConversionFailedException conversionFailure = findCause(cause, ConversionFailedException.class);
		if (conversionFailure != null) {
			String message = "failed to convert " + conversionFailure.getSourceType() + " to "
					+ conversionFailure.getTargetType();
			if (rootCause != null) {
				message += " (caused by " + getExceptionTypeAndMessage(rootCause) + ")";
			}
			return message;
		}
		if (rootCause != null && StringUtils.hasText(rootCause.getMessage())) {
			return getExceptionTypeAndMessage(rootCause);
		}
		return getExceptionTypeAndMessage(cause);
	}

	private Throwable getRootCause(Throwable cause) {
		Throwable rootCause = cause;
		while (rootCause != null && rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		return rootCause;
	}

	private String getExceptionTypeAndMessage(Throwable ex) {
		String message = ex.getMessage();
		return ex.getClass().getName() + (StringUtils.hasText(message) ? ": " + message : "");
	}

	private FailureAnalysis getFailureAnalysis(Object description, BindException cause) {
		StringBuilder message = new StringBuilder("Update your application's configuration");
		Collection<String> validValues = findValidValues(cause);
		if (!validValues.isEmpty()) {
			message.append(String.format(". The following values are valid:%n"));
			validValues.forEach((value) -> message.append(String.format("%n    %s", value)));
		}
		return new FailureAnalysis(description.toString(), message.toString(), cause);
	}

	private Collection<String> findValidValues(BindException ex) {
		ConversionFailedException conversionFailure = findCause(ex, ConversionFailedException.class);
		if (conversionFailure != null) {
			Object[] enumConstants = conversionFailure.getTargetType().getType().getEnumConstants();
			if (enumConstants != null) {
				return Stream.of(enumConstants).map(Object::toString).collect(Collectors.toCollection(TreeSet::new));
			}
		}
		return Collections.emptySet();
	}

}
