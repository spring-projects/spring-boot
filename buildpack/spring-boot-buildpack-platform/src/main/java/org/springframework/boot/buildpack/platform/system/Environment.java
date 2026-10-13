/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.system;

import org.jspecify.annotations.Nullable;

/**
 * Provides access to environment variable values.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.3.0
 */
@FunctionalInterface
public interface Environment {

	/**
	 * Standard {@link Environment} implementation backed by
	 * {@link System#getenv(String)}.
	 */
	Environment SYSTEM = System::getenv;

	/**
	 * Gets the value of the specified environment variable.
	 * @param name the name of the environment variable
	 * @return the string value of the variable, or {@code null} if the variable is not
	 * defined in the environment
	 */
	@Nullable String get(String name);

}
