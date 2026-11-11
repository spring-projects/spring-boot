/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.service.connection;

/**
 * {@link RuntimeException} thrown when required {@link ConnectionDetails} could not be
 * found.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class ConnectionDetailsNotFoundException extends RuntimeException {

	<S> ConnectionDetailsNotFoundException(S source) {
		this("No ConnectionDetails found for source '%s'".formatted(source));
	}

	public ConnectionDetailsNotFoundException(String message) {
		super(message);
	}

	public ConnectionDetailsNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
