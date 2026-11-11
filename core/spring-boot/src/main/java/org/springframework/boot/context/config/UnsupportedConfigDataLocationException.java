/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

/**
 * Exception throw if a {@link ConfigDataLocation} is not supported.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public class UnsupportedConfigDataLocationException extends ConfigDataException {

	private final ConfigDataLocation location;

	/**
	 * Create a new {@link UnsupportedConfigDataLocationException} instance.
	 * @param location the unsupported location
	 */
	UnsupportedConfigDataLocationException(ConfigDataLocation location) {
		super("Unsupported config data location '" + location + "'", null);
		this.location = location;
	}

	/**
	 * Return the unsupported location reference.
	 * @return the unsupported location reference
	 */
	public ConfigDataLocation getLocation() {
		return this.location;
	}

}
