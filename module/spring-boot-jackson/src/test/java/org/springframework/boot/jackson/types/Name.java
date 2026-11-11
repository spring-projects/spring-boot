/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.types;

import org.jspecify.annotations.Nullable;

/**
 * Sample object used for tests.
 *
 * @author Paul Aly
 */
public class Name {

	protected final @Nullable String name;

	public Name(@Nullable String name) {
		this.name = name;
	}

	public @Nullable String getName() {
		return this.name;
	}

}
