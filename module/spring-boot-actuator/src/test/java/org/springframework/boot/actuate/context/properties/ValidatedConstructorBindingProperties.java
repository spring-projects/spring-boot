/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.context.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Used for testing the {@link ConfigurationPropertiesReportEndpoint} endpoint with
 * validated {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 */
@Validated
@ConfigurationProperties("validated")
public class ValidatedConstructorBindingProperties {

	private final String name;

	ValidatedConstructorBindingProperties(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
