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

	/**
     * Analyzes the given root failure and cause to determine if it is a bind validation exception.
     * If it is a bind validation exception, it returns a FailureAnalysis object with the analysis result.
     * Otherwise, it returns null.
     *
     * @param rootFailure The root failure that occurred.
     * @param cause The cause of the root failure.
     * @return A FailureAnalysis object if the exception is a bind validation exception, otherwise null.
     */
    @Override
	protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
		ExceptionDetails details = getBindValidationExceptionDetails(rootFailure);
		if (details == null) {
			return null;
		}
		return analyzeBindValidationException(details);
	}

	/**
     * Retrieves the details of a bind validation exception.
     * 
     * @param rootFailure the root failure exception
     * @return the exception details, or null if no bind validation exception is found
     */
    private ExceptionDetails getBindValidationExceptionDetails(Throwable rootFailure) {
		BindValidationException validationException = findCause(rootFailure, BindValidationException.class);
		if (validationException != null) {
			BindException target = findCause(rootFailure, BindException.class);
			List<ObjectError> errors = validationException.getValidationErrors().getAllErrors();
			return new ExceptionDetails(errors, target, validationException);
		}
		org.springframework.validation.BindException bindException = findCause(rootFailure,
				org.springframework.validation.BindException.class);
		if (bindException != null) {
			List<ObjectError> errors = bindException.getAllErrors();
			return new ExceptionDetails(errors, bindException.getTarget(), bindException);
		}
		return null;
	}

	/**
     * Analyzes a BindValidationException and generates a FailureAnalysis object.
     * 
     * @param details the ExceptionDetails object containing information about the exception
     * @return a FailureAnalysis object representing the analysis result
     */
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

	/**
     * Appends a field error to the description.
     * 
     * @param description the StringBuilder object to append the error description to
     * @param error the FieldError object representing the error
     */
    private void appendFieldError(StringBuilder description, FieldError error) {
		Origin origin = Origin.from(error);
		description.append(String.format("%n    Property: %s", error.getObjectName() + "." + error.getField()));
		description.append(String.format("%n    Value: \"%s\"", error.getRejectedValue()));
		if (origin != null) {
			description.append(String.format("%n    Origin: %s", origin));
		}
	}

	/**
     * Returns a FailureAnalysis object based on the provided description and cause.
     * 
     * @param description the description of the failure
     * @param cause the Throwable object representing the cause of the failure
     * @return a FailureAnalysis object containing the failure details
     */
    private FailureAnalysis getFailureAnalysis(Object description, Throwable cause) {
		return new FailureAnalysis(description.toString(), "Update your application's configuration", cause);
	}

	/**
     * ExceptionDetails class.
     */
    private static class ExceptionDetails {

		private final List<ObjectError> errors;

		private final Object target;

		private final Throwable cause;

		/**
         * Constructs a new ExceptionDetails object with the specified errors, target, and cause.
         * 
         * @param errors the list of ObjectError containing the errors associated with the exception
         * @param target the target object that caused the exception
         * @param cause the Throwable object that caused the exception
         */
        ExceptionDetails(List<ObjectError> errors, Object target, Throwable cause) {
			this.errors = errors;
			this.target = target;
			this.cause = cause;
		}

		/**
         * Returns the target object associated with this ExceptionDetails.
         *
         * @return the target object
         */
        Object getTarget() {
			return this.target;
		}

		/**
         * Returns the list of ObjectError instances representing the errors.
         *
         * @return the list of ObjectError instances representing the errors
         */
        List<ObjectError> getErrors() {
			return this.errors;
		}

		/**
         * Returns the cause of this exception.
         *
         * @return the cause of this exception, or null if the cause is nonexistent or unknown.
         */
        Throwable getCause() {
			return this.cause;
		}

	}

}
