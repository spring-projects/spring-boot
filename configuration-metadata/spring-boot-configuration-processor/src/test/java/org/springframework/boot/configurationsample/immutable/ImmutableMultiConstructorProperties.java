/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import org.springframework.boot.configurationsample.TestConstructorBinding;

/**
 * Simple immutable properties with several constructors.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutableMultiConstructorProperties {

	private final String name;

	/**
	 * Test description.
	 */
	private final String description;

	public ImmutableMultiConstructorProperties(String name) {
		this(name, null);
	}

	@TestConstructorBinding
	public ImmutableMultiConstructorProperties(String name, String description) {
		this.name = name;
		this.description = description;
	}

}
