/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.types;

import org.jspecify.annotations.Nullable;

/**
 * Sample object used for tests.
 *
 * @author Paul Aly
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public class Name {

	protected final @Nullable String name;

	public Name(@Nullable String name) {
		this.name = name;
	}

	public @Nullable String getName() {
		return this.name;
	}

}
