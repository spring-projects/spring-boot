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

	/**
     * Analyzes the given BindException and its root cause to determine the failure analysis.
     * 
     * @param rootFailure the root failure that occurred
     * @param cause the BindException that caused the failure
     * @return the FailureAnalysis object representing the analysis result, or null if the exception is not relevant
     */
    @Override
	protected FailureAnalysis analyze(Throwable rootFailure, BindException cause) {
		Throwable rootCause = cause.getCause();
		if (rootCause instanceof BindValidationException
				|| rootCause instanceof UnboundConfigurationPropertiesException) {
			return null;
		}
		return analyzeGenericBindException(rootFailure, cause);
	}

	/**
     * Analyzes a generic BindException and generates a FailureAnalysis object.
     * 
     * @param rootFailure the root cause of the exception
     * @param cause the BindException that occurred
     * @return a FailureAnalysis object containing the analysis result
     */
    private FailureAnalysis analyzeGenericBindException(Throwable rootFailure, BindException cause) {
		FailureAnalysis missingParametersAnalysis = MissingParameterNamesFailureAnalyzer
			.analyzeForMissingParameters(rootFailure);
		StringBuilder description = new StringBuilder(String.format("%s:%n", cause.getMessage()));
		ConfigurationProperty property = cause.getProperty();
		buildDescription(description, property);
		description.append(String.format("%n    Reason: %s", getMessage(cause)));
		if (missingParametersAnalysis != null) {
			MissingParameterNamesFailureAnalyzer.appendPossibility(description);
		}
		return getFailureAnalysis(description.toString(), cause, missingParametersAnalysis);
	}

	/**
     * Builds the description for a given configuration property.
     * 
     * @param description the StringBuilder object to append the description to
     * @param property the ConfigurationProperty object to build the description for
     */
    private void buildDescription(StringBuilder description, ConfigurationProperty property) {
		if (property != null) {
			description.append(String.format("%n    Property: %s", property.getName()));
			description.append(String.format("%n    Value: \"%s\"", property.getValue()));
			description.append(String.format("%n    Origin: %s", property.getOrigin()));
		}
	}

	/**
     * Returns the error message for a BindException.
     * 
     * @param cause the BindException that occurred
     * @return the error message
     */
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

	/**
     * Returns the root cause of the given Throwable.
     * 
     * @param cause the Throwable to find the root cause for
     * @return the root cause of the given Throwable
     */
    private Throwable getRootCause(Throwable cause) {
		Throwable rootCause = cause;
		while (rootCause != null && rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}
		return rootCause;
	}

	/**
     * Returns the exception type and message of the given Throwable object.
     * 
     * @param ex the Throwable object to get the exception type and message from
     * @return a String containing the exception type and message in the format "ExceptionType: Message"
     */
    private String getExceptionTypeAndMessage(Throwable ex) {
		String message = ex.getMessage();
		return ex.getClass().getName() + (StringUtils.hasText(message) ? ": " + message : "");
	}

	/**
     * Generates a failure analysis for a bind exception.
     * 
     * @param description              the description of the failure
     * @param cause                    the bind exception that caused the failure
     * @param missingParametersAnalysis the failure analysis for missing parameters, if any
     * @return the failure analysis for the bind exception
     */
    private FailureAnalysis getFailureAnalysis(String description, BindException cause,
			FailureAnalysis missingParametersAnalysis) {
		StringBuilder action = new StringBuilder("Update your application's configuration");
		Collection<String> validValues = findValidValues(cause);
		if (!validValues.isEmpty()) {
			action.append(String.format(". The following values are valid:%n"));
			validValues.forEach((value) -> action.append(String.format("%n    %s", value)));
		}
		if (missingParametersAnalysis != null) {
			action.append(String.format("%n%n%s", missingParametersAnalysis.getAction()));
		}
		return new FailureAnalysis(description, action.toString(), cause);
	}

	/**
     * Finds the valid values for a given BindException.
     * 
     * @param ex the BindException to analyze
     * @return a collection of valid values as strings
     */
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
