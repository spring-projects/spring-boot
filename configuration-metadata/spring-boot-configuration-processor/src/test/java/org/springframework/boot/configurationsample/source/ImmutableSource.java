/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

/**
 * Immutable type with manual metadata. This illustrates the case where the type of a
 * property is defined in a separate class and source-based metadata cannot be discovered.
 */
public class ImmutableSource {

	@SuppressWarnings("unused")
	private final String name;

	@SuppressWarnings("unused")
	private final String description;

	@SuppressWarnings("unused")
	private final String type;

	public ImmutableSource(String name, String description, String type) {
		this.name = name;
		this.description = description;
		this.type = type;
	}

}
