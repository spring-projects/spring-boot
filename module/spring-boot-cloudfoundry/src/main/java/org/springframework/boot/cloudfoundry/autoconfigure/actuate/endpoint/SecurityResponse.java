/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;

/**
 * Response from the Cloud Foundry security interceptors.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
public class SecurityResponse {

	private final HttpStatus status;

	private final @Nullable String message;

	public SecurityResponse(HttpStatus status) {
		this(status, null);
	}

	public SecurityResponse(HttpStatus status, @Nullable String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return this.status;
	}

	public @Nullable String getMessage() {
		return this.message;
	}

	public static SecurityResponse success() {
		return new SecurityResponse(HttpStatus.OK);
	}

}
