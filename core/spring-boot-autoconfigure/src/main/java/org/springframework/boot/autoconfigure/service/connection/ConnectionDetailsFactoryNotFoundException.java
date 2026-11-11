/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.service.connection;

/**
 * {@link RuntimeException} thrown when a {@link ConnectionDetailsFactory} could not be
 * found.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class ConnectionDetailsFactoryNotFoundException extends RuntimeException {

	<S> ConnectionDetailsFactoryNotFoundException(S source) {
		this("No ConnectionDetailsFactory found for source '%s'".formatted(source));
	}

	public ConnectionDetailsFactoryNotFoundException(String message) {
		super(message);
	}

	public ConnectionDetailsFactoryNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
