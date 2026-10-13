/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.server;

/**
 * Exceptions thrown by an RSocket server.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class RSocketServerException extends RuntimeException {

	public RSocketServerException(String message, Throwable cause) {
		super(message, cause);
	}

}
