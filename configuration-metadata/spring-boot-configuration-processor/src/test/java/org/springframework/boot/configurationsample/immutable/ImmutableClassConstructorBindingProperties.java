/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

/**
 * Simple immutable properties with several constructors.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutableClassConstructorBindingProperties {

	private final String name;

	private final String description;

	public ImmutableClassConstructorBindingProperties(String name, String description) {
		this.name = name;
		this.description = description;
	}

}
