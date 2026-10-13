/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Exception thrown when a configuration property value is invalid.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@SuppressWarnings("serial")
public class InvalidConfigurationPropertyValueException extends RuntimeException {

	private final String name;

	private final @Nullable Object value;

	private final @Nullable String reason;

	/**
	 * Creates a new instance for the specified property {@code name} and {@code value},
	 * including a {@code reason} why the value is invalid.
	 * @param name the name of the property in canonical format
	 * @param value the value of the property, can be {@code null}
	 * @param reason a human-readable text that describes why the value is invalid. Starts
	 * with an upper-case character and ends with a dot. Several sentences and lines are
	 * allowed.
	 */
	public InvalidConfigurationPropertyValueException(String name, @Nullable Object value, @Nullable String reason) {
		this(name, value, reason, null);
	}

	/**
	 * Creates a new instance for the specified property {@code name} and {@code value},
	 * including a {@code reason} why the value is invalid.
	 * @param name the name of the property in canonical format
	 * @param value the value of the property, can be {@code null}
	 * @param reason a human-readable text that describes why the value is invalid. Starts
	 * with an upper-case character and ends with a dot. Several sentences and lines are
	 * allowed.
	 * @param cause the cause of the exception or {@code null}
	 * @since 4.1.0
	 */
	public InvalidConfigurationPropertyValueException(String name, @Nullable Object value, @Nullable String reason,
			@Nullable Throwable cause) {
		super("Property " + name + " with value '" + value + "' is invalid: " + reason, cause);
		Assert.notNull(name, "'name' must not be null");
		this.name = name;
		this.value = value;
		this.reason = reason;
	}

	/**
	 * Return the name of the property.
	 * @return the property name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the invalid value, can be {@code null}.
	 * @return the invalid value
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

	/**
	 * Return the reason why the value is invalid.
	 * @return the reason
	 */
	public @Nullable String getReason() {
		return this.reason;
	}

}
