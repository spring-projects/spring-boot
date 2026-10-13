/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Exception thrown when connection to the Docker daemon fails.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class DockerConnectionException extends RuntimeException {

	private static final String JNA_EXCEPTION_CLASS_NAME = "com.sun.jna.LastErrorException";

	public DockerConnectionException(String host, Exception cause) {
		super(buildMessage(host, cause), cause);
	}

	private static String buildMessage(String host, Exception cause) {
		Assert.notNull(host, "'host' must not be null");
		Assert.notNull(cause, "'cause' must not be null");
		StringBuilder message = new StringBuilder("Connection to the Docker daemon at '" + host + "' failed");
		String causeMessage = getCauseMessage(cause);
		if (StringUtils.hasText(causeMessage)) {
			message.append(" with error \"").append(causeMessage).append("\"");
		}
		message.append("; ensure the Docker daemon is running and accessible");
		return message.toString();
	}

	private static @Nullable String getCauseMessage(Exception cause) {
		if (cause.getCause() != null && cause.getCause().getClass().getName().equals(JNA_EXCEPTION_CLASS_NAME)) {
			return cause.getCause().getMessage();
		}
		return cause.getMessage();
	}

}
