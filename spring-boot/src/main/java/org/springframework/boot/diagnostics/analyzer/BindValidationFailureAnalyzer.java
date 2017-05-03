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

import java.util.List;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.origin.Origin;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of any bind validation
 * failures caused by {@link BindValidationException} or
 * {@link org.springframework.validation.BindException}.
 *
 * @author Madhura Bhave
 */
class BindValidationFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
		ExceptionDetails details = getBindValidationExceptionDetails(rootFailure);
		if (details == null) {
			return null;
		}
		return analyzeBindValidationException(details);
	}

	private ExceptionDetails getBindValidationExceptionDetails(Throwable rootFailure) {
		BindValidationException validationException = findCause(rootFailure,
				BindValidationException.class);
		if (validationException != null) {
			BindException target = findCause(rootFailure, BindException.class);
			List<ObjectError> errors = validationException.getValidationErrors()
					.getAllErrors();
			return new ExceptionDetails(errors, target, validationException);
		}
		org.springframework.validation.BindException bindException = findCause(
				rootFailure, org.springframework.validation.BindException.class);
		if (bindException != null) {
			List<ObjectError> errors = bindException.getAllErrors();
			return new ExceptionDetails(errors, bindException.getTarget(), bindException);
		}
		return null;
	}

	private FailureAnalysis analyzeBindValidationException(ExceptionDetails details) {
		StringBuilder description = new StringBuilder(
				String.format("Binding to target %s failed:%n", details.getTarget()));
		for (ObjectError error : details.getErrors()) {
			if (error instanceof FieldError) {
				appendFieldError(description, (FieldError) error);
			}
			description.append(
					String.format("%n    Reason: %s%n", error.getDefaultMessage()));
		}
		return getFailureAnalysis(description, details.getCause());
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

	private FailureAnalysis getFailureAnalysis(Object description, Throwable cause) {
		return new FailureAnalysis(description.toString(),
				"Update your application's configuration", cause);
	}

	private static class ExceptionDetails {

		private List<ObjectError> errors;

		private Object target;

		private Throwable cause;

		ExceptionDetails(List<ObjectError> errors, Object target, Throwable cause) {
			this.errors = errors;
			this.target = target;
			this.cause = cause;
		}

		public Object getTarget() {
			return this.target;
		}

		public List<ObjectError> getErrors() {
			return this.errors;
		}

		public Throwable getCause() {
			return this.cause;
		}

	}

}
