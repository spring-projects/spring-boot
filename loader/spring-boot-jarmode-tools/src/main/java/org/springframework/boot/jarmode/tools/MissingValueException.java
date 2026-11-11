/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

/**
 * Exception thrown when a required value is not provided for an option.
 *
 * @author Scott Frederick
 */
class MissingValueException extends RuntimeException {

	private final String optionName;

	MissingValueException(String optionName) {
		this.optionName = optionName;
	}

	@Override
	public String getMessage() {
		return "--" + this.optionName;
	}

}
