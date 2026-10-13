/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Exception thrown to indicate a Builder error.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class BuilderException extends RuntimeException {

	private final @Nullable String operation;

	private final int statusCode;

	BuilderException(@Nullable String operation, int statusCode) {
		super(buildMessage(operation, statusCode));
		this.operation = operation;
		this.statusCode = statusCode;
	}

	/**
	 * Return the Builder operation that failed.
	 * @return the operation description
	 */
	public @Nullable String getOperation() {
		return this.operation;
	}

	/**
	 * Return the status code returned from a Builder operation.
	 * @return the statusCode the status code
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	private static String buildMessage(@Nullable String operation, int statusCode) {
		StringBuilder message = new StringBuilder("Builder");
		if (StringUtils.hasLength(operation)) {
			message.append(" lifecycle '").append(operation).append("'");
		}
		message.append(" failed with status code ").append(statusCode);
		return message.toString();
	}

}
