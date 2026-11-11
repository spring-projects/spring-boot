/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.origin.Origin;
import org.springframework.util.Assert;

/**
 * {@link ConfigDataNotFoundException} thrown when a {@link ConfigDataLocation} cannot be
 * found.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class ConfigDataLocationNotFoundException extends ConfigDataNotFoundException {

	private final ConfigDataLocation location;

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that could not be found
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location) {
		this(location, null);
	}

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that could not be found
	 * @param cause the exception cause
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location, @Nullable Throwable cause) {
		this(location, getMessage(location), cause);
	}

	/**
	 * Create a new {@link ConfigDataLocationNotFoundException} instance.
	 * @param location the location that could not be found
	 * @param message the exception message
	 * @param cause the exception cause
	 * @since 2.4.7
	 */
	public ConfigDataLocationNotFoundException(ConfigDataLocation location, String message, @Nullable Throwable cause) {
		super(message, cause);
		Assert.notNull(location, "'location' must not be null");
		this.location = location;
	}

	/**
	 * Return the location that could not be found.
	 * @return the location
	 */
	public ConfigDataLocation getLocation() {
		return this.location;
	}

	@Override
	public @Nullable Origin getOrigin() {
		return Origin.from(this.location);
	}

	@Override
	public String getReferenceDescription() {
		return getReferenceDescription(this.location);
	}

	private static String getMessage(ConfigDataLocation location) {
		return String.format("Config data %s cannot be found", getReferenceDescription(location));
	}

	private static String getReferenceDescription(ConfigDataLocation location) {
		return String.format("location '%s'", location);
	}

}
