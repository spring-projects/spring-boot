/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Context passed to the {@link LoggingSystem} during initialization.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class LoggingInitializationContext {

	private final @Nullable ConfigurableEnvironment environment;

	/**
	 * Create a new {@link LoggingInitializationContext} instance.
	 * @param environment the Spring environment.
	 */
	public LoggingInitializationContext(@Nullable ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the Spring environment if available.
	 * @return the {@link Environment} or {@code null}
	 */
	public @Nullable Environment getEnvironment() {
		return this.environment;
	}

}
