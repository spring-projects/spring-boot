/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import java.util.Optional;

/**
 * Data object with a public method picked up by the {@code ObjectToObjectConverter}. Used
 * in {@link ConfigurationPropertiesTests}.
 *
 * @author Phillip Webb
 */
public class WithPublicObjectToObjectMethod {

	private final String value;

	WithPublicObjectToObjectMethod(String value) {
		this.value = value;
	}

	String getValue() {
		return this.value;
	}

	public static Optional<WithPublicObjectToObjectMethod> from(String value) {
		return Optional.of(new WithPublicObjectToObjectMethod(value));
	}

}
