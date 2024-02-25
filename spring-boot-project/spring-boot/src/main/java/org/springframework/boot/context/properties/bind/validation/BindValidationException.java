/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.bind.validation;

import org.springframework.util.Assert;

/**
 * Error thrown when validation fails during a bind operation.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see ValidationErrors
 * @see ValidationBindHandler
 */
public class BindValidationException extends RuntimeException {

	private final ValidationErrors validationErrors;

	/**
     * Constructs a new BindValidationException with the specified validation errors.
     * 
     * @param validationErrors the validation errors associated with the exception
     * @throws IllegalArgumentException if the validationErrors parameter is null
     */
    BindValidationException(ValidationErrors validationErrors) {
		super(getMessage(validationErrors));
		Assert.notNull(validationErrors, "ValidationErrors must not be null");
		this.validationErrors = validationErrors;
	}

	/**
	 * Return the validation errors that caused the exception.
	 * @return the validationErrors the validation errors
	 */
	public ValidationErrors getValidationErrors() {
		return this.validationErrors;
	}

	/**
     * Returns a message containing the binding validation errors.
     * 
     * @param errors the validation errors to be included in the message
     * @return a string message containing the binding validation errors
     */
    private static String getMessage(ValidationErrors errors) {
		StringBuilder message = new StringBuilder("Binding validation errors");
		if (errors != null) {
			message.append(" on ").append(errors.getName());
			errors.getAllErrors().forEach((error) -> message.append(String.format("%n   - %s", error)));
		}
		return message.toString();
	}

}
