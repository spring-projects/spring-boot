/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.docker.compose.core.RunningService;

/**
 * Exception thrown if readiness checking has timed out.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public final class ReadinessTimeoutException extends RuntimeException {

	private final Duration timeout;

	ReadinessTimeoutException(Duration timeout, List<ServiceNotReadyException> exceptions) {
		super(buildMessage(timeout, exceptions));
		this.timeout = timeout;
		exceptions.forEach(this::addSuppressed);
	}

	private static String buildMessage(Duration timeout, List<ServiceNotReadyException> exceptions) {
		List<String> serviceNames = exceptions.stream()
			.map(ServiceNotReadyException::getService)
			.filter(Objects::nonNull)
			.map(RunningService::name)
			.toList();
		return "Readiness timeout of %s reached while waiting for services %s".formatted(timeout, serviceNames);
	}

	/**
	 * Return the timeout that was reached.
	 * @return the timeout
	 */
	public Duration getTimeout() {
		return this.timeout;
	}

}
