/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;

/**
 * Exception thrown when a single {@link RunningService} is not ready.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceNotReadyException extends RuntimeException {

	private final RunningService service;

	ServiceNotReadyException(RunningService service, String message) {
		this(service, message, null);
	}

	ServiceNotReadyException(RunningService service, String message, @Nullable Throwable cause) {
		super(message, cause);
		this.service = service;
	}

	RunningService getService() {
		return this.service;
	}

}
