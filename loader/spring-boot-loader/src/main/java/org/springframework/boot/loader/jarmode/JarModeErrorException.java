/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.jarmode;

/**
 * Simple {@link RuntimeException} used to fail the jar mode with a simple printed error
 * message.
 *
 * @author Phillip Webb
 * @since 3.3.7
 */
public class JarModeErrorException extends RuntimeException {

	public JarModeErrorException(String message) {
		super(message);
	}

	public JarModeErrorException(String message, Throwable cause) {
		super(message, cause);
	}

}
