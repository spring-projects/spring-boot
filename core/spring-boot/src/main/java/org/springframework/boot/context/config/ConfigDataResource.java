/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

/**
 * A single resource from which {@link ConfigData} can be loaded. Implementations must
 * implement a valid {@link #equals(Object) equals}, {@link #hashCode() hashCode} and
 * {@link #toString() toString} methods.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public abstract class ConfigDataResource {

	private final boolean optional;

	/**
	 * Create a new non-optional {@link ConfigDataResource} instance.
	 */
	public ConfigDataResource() {
		this(false);
	}

	/**
	 * Create a new {@link ConfigDataResource} instance.
	 * @param optional if the resource is optional
	 * @since 2.4.6
	 */
	protected ConfigDataResource(boolean optional) {
		this.optional = optional;
	}

	boolean isOptional() {
		return this.optional;
	}

}
