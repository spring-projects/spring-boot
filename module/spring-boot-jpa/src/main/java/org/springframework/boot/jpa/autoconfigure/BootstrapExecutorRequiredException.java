/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jpa.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.Assert;

/**
 * Exception thrown then the auto-configured
 * {@link LocalContainerEntityManagerFactoryBean} is missing a required bootstrap
 * executor.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
public class BootstrapExecutorRequiredException extends IllegalStateException {

	private final @Nullable String propertyName;

	private final @Nullable String propertyValue;

	public BootstrapExecutorRequiredException(String message) {
		this(message, null);
	}

	public BootstrapExecutorRequiredException(String message, @Nullable Throwable cause) {
		this(message, null, null, cause);
	}

	private BootstrapExecutorRequiredException(String message, @Nullable String propertyName,
			@Nullable String propertyValue, @Nullable Throwable cause) {
		super(message, cause);
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	@Nullable String getPropertyName() {
		return this.propertyName;
	}

	@Nullable String getPropertyValue() {
		return this.propertyValue;
	}

	public static BootstrapExecutorRequiredException ofProperty(String name, String value) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(value, "'value' must not be empty");
		String message = "An EntityManagerFactoryBean bootstrap executor is required when '%s' is set to '%s'"
			.formatted(name, value);
		return new BootstrapExecutorRequiredException(message, name, value, null);
	}

}
