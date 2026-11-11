/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for configuration data exceptions.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public abstract class ConfigDataException extends RuntimeException {

	/**
	 * Create a new {@link ConfigDataException} instance.
	 * @param message the exception message
	 * @param cause the exception cause
	 */
	protected ConfigDataException(String message, @Nullable Throwable cause) {
		super(message, cause);
	}

}
