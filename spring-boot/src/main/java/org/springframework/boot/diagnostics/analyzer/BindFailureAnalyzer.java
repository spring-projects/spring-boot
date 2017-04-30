/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.UnboundConfigurationPropertiesException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.origin.Origin;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BindException}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
class BindFailureAnalyzer extends AbstractFailureAnalyzer<BindException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, BindException cause) {
		if (cause.getCause() instanceof BindValidationException) {
			return analyzeBindValidationException(cause,
					(BindValidationException) cause.getCause());
		}
		else if (cause.getCause() instanceof UnboundConfigurationPropertiesException) {
			return analyzeUnboundConfigurationPropertiesException(cause,
					(UnboundConfigurationPropertiesException) cause.getCause());
		}
		return analyzeGenericBindException(cause);
	}

	private FailureAnalysis analyzeBindValidationException(BindException cause,
			BindValidationException validationException) {
		ValidationErrors errors = validationException.getValidationErrors();
		if (!errors.hasErrors()) {
			return null;
		}
		StringBuilder description = new StringBuilder(
				String.format("Binding to target %s failed:%n", cause.getTarget()));
		for (ObjectError error : errors) {
			if (error instanceof FieldError) {
				appendFieldError(description, (FieldError) error);
			}
			description.append(
					String.format("%n    Reason: %s%n", error.getDefaultMessage()));
		}
		return getFailureAnalysis(description, cause);
	}

	private void appendFieldError(StringBuilder description, FieldError error) {
		Origin origin = Origin.from(error);
		description.append(String.format("%n    Property: %s",
				error.getObjectName() + "." + error.getField()));
		description.append(String.format("%n    Value: %s", error.getRejectedValue()));
		if (origin != null) {
			description.append(String.format("%n    Origin: %s", origin));
		}
	}

	private FailureAnalysis analyzeUnboundConfigurationPropertiesException(
			BindException cause, UnboundConfigurationPropertiesException exception) {
		StringBuilder description = new StringBuilder(
				String.format("Binding to target %s failed:%n", cause.getTarget()));
		for (ConfigurationProperty property : exception.getUnboundProperties()) {
			buildDescription(description, property);
			description.append(String.format("%n    Reason: %s", exception.getMessage()));
		}
		return getFailureAnalysis(description, cause);
	}

	private FailureAnalysis analyzeGenericBindException(BindException cause) {
		StringBuilder description = new StringBuilder(
				String.format("Binding to target %s failed:%n", cause.getTarget()));
		ConfigurationProperty property = cause.getProperty();
		buildDescription(description, property);
		description.append(String.format("%n    Reason: %s", getMessage(cause)));
		return getFailureAnalysis(description, cause);
	}

	private void buildDescription(StringBuilder description,
			ConfigurationProperty property) {
		if (property != null) {
			description.append(String.format("%n    Property: %s", property.getName()));
			description.append(String.format("%n    Value: %s", property.getValue()));
			description.append(String.format("%n    Origin: %s", property.getOrigin()));
		}
	}

	private String getMessage(BindException cause) {
		if (cause.getCause() != null
				&& StringUtils.hasText(cause.getCause().getMessage())) {
			return cause.getCause().getMessage();
		}
		return cause.getMessage();
	}

	private FailureAnalysis getFailureAnalysis(Object description, BindException cause) {
		return new FailureAnalysis(description.toString(),
				"Update your application's configuration", cause);
	}

}
