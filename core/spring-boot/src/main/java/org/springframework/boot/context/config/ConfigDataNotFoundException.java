/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.OriginProvider;

/**
 * {@link ConfigDataNotFoundException} thrown when a {@link ConfigData} cannot be found.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public abstract class ConfigDataNotFoundException extends ConfigDataException implements OriginProvider {

	/**
	 * Create a new {@link ConfigDataNotFoundException} instance.
	 * @param message the exception message
	 * @param cause the exception cause
	 */
	ConfigDataNotFoundException(String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	/**
	 * Return a description of actual referenced item that could not be found.
	 * @return a description of the referenced items
	 */
	public abstract String getReferenceDescription();

}
