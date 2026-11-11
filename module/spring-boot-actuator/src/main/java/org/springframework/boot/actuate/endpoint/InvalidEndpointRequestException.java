/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import org.jspecify.annotations.Nullable;

/**
 * Indicate that an endpoint request is invalid.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class InvalidEndpointRequestException extends RuntimeException {

	private final @Nullable String reason;

	public InvalidEndpointRequestException(String message, @Nullable String reason) {
		super(message);
		this.reason = reason;
	}

	public InvalidEndpointRequestException(String message, @Nullable String reason, @Nullable Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	/**
	 * Return the reason explaining why the request is invalid, potentially {@code null}.
	 * @return the reason for the failure
	 */
	public @Nullable String getReason() {
		return this.reason;
	}

}
