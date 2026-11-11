/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cli.command.init;

/**
 * Exception with a message that can be reported to the user.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ReportableException extends RuntimeException {

	public ReportableException(String message) {
		super(message);
	}

	public ReportableException(String message, Throwable cause) {
		super(message, cause);
	}

}
