/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.origin.Origin;
import org.springframework.util.Assert;
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
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
		ExceptionDetails details = getBindValidationExceptionDetails(rootFailure);
		if (details == null) {
			return null;
		}
		return analyzeBindValidationException(details);
	}

	private @Nullable ExceptionDetails getBindValidationExceptionDetails(Throwable rootFailure) {
		BindValidationException validationException = findCause(rootFailure, BindValidationException.class);
		if (validationException != null) {
			BindException bindException = findCause(rootFailure, BindException.class);
			List<ObjectError> errors = validationException.getValidationErrors().getAllErrors();
			Assert.state(bindException != null, "BindException not found");
			return new ExceptionDetails(errors, bindException.getTarget().getType(), validationException);
		}
		org.springframework.validation.BindException bindException = findCause(rootFailure,
				org.springframework.validation.BindException.class);
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
			if (error instanceof FieldError fieldError) {
				appendFieldError(description, fieldError);
			}
			description.append(String.format("%n    Reason: %s%n", error.getDefaultMessage()));
		}
		return getFailureAnalysis(description, details.getCause());
	}

	private void appendFieldError(StringBuilder description, FieldError error) {
		Origin origin = Origin.from(error);
		description.append(String.format("%n    Property: %s", error.getObjectName() + "." + error.getField()));
		description.append(String.format("%n    Value: \"%s\"", error.getRejectedValue()));
		if (origin != null) {
			description.append(String.format("%n    Origin: %s", origin));
		}
	}

	private FailureAnalysis getFailureAnalysis(Object description, Throwable cause) {
		return new FailureAnalysis(description.toString(), "Update your application's configuration", cause);
	}

	private static class ExceptionDetails {

		private final List<ObjectError> errors;

		private final @Nullable Object target;

		private final Throwable cause;

		ExceptionDetails(List<ObjectError> errors, @Nullable Object target, Throwable cause) {
			this.errors = errors;
			this.target = target;
			this.cause = cause;
		}

		@Nullable Object getTarget() {
			return this.target;
		}

		List<ObjectError> getErrors() {
			return this.errors;
		}

		Throwable getCause() {
			return this.cause;
		}

	}

}
