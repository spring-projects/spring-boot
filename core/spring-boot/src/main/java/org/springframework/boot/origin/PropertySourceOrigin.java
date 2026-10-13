/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.origin;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * {@link Origin} from a {@link PropertySource}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class PropertySourceOrigin implements Origin, OriginProvider {

	private final PropertySource<?> propertySource;

	private final String propertyName;

	private final @Nullable Origin origin;

	/**
	 * Create a new {@link PropertySourceOrigin} instance.
	 * @param propertySource the property source
	 * @param propertyName the name from the property source
	 */
	public PropertySourceOrigin(PropertySource<?> propertySource, String propertyName) {
		this(propertySource, propertyName, null);
	}

	/**
	 * Create a new {@link PropertySourceOrigin} instance.
	 * @param propertySource the property source
	 * @param propertyName the name from the property source
	 * @param origin the actual origin for the source if known
	 * @since 3.2.8
	 */
	public PropertySourceOrigin(PropertySource<?> propertySource, String propertyName, @Nullable Origin origin) {
		Assert.notNull(propertySource, "'propertySource' must not be null");
		Assert.hasLength(propertyName, "'propertyName' must not be empty");
		this.propertySource = propertySource;
		this.propertyName = propertyName;
		this.origin = origin;
	}

	/**
	 * Return the origin {@link PropertySource}.
	 * @return the origin property source
	 */
	public PropertySource<?> getPropertySource() {
		return this.propertySource;
	}

	/**
	 * Return the property name that was used when obtaining the original value from the
	 * {@link #getPropertySource() property source}.
	 * @return the origin property name
	 */
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * Return the actual origin for the source if known.
	 * @return the actual source origin
	 * @since 3.2.8
	 */
	@Override
	public @Nullable Origin getOrigin() {
		return this.origin;
	}

	@Override
	public @Nullable Origin getParent() {
		return (this.origin != null) ? this.origin.getParent() : null;
	}

	@Override
	public String toString() {
		return (this.origin != null) ? this.origin.toString()
				: "\"" + this.propertyName + "\" from property source \"" + this.propertySource.getName() + "\"";
	}

	/**
	 * Get an {@link Origin} for the given {@link PropertySource} and
	 * {@code propertyName}. Will either return an {@link OriginLookup} result or a
	 * {@link PropertySourceOrigin}.
	 * @param propertySource the origin property source
	 * @param name the property name
	 * @return the property origin
	 */
	public static Origin get(PropertySource<?> propertySource, String name) {
		Origin origin = OriginLookup.getOrigin(propertySource, name);
		return (origin instanceof PropertySourceOrigin) ? origin
				: new PropertySourceOrigin(propertySource, name, origin);
	}

}
