/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jarmode.tools;

/**
 * Exception thrown when an unrecognized option is encountered.
 *
 * @author Scott Frederick
 */
class UnknownOptionException extends RuntimeException {

	private final String optionName;

	UnknownOptionException(String optionName) {
		this.optionName = optionName;
	}

	@Override
	public String getMessage() {
		return "--" + this.optionName;
	}

}
