/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.origin;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;
import org.springframework.util.Assert;

/**
 * Mock {@link Origin} implementation used for testing.
 *
 * @author Phillip Webb
 */
public final class MockOrigin implements Origin {

	private final String value;

	private final @Nullable Origin parent;

	private MockOrigin(String value, @Nullable Origin parent) {
		Assert.notNull(value, "'value' must not be null");
		this.value = value;
		this.parent = parent;
	}

	@Override
	public @Nullable Origin getParent() {
		return this.parent;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value.equals(((MockOrigin) obj).value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	@Contract("!null -> !null")
	public static @Nullable Origin of(@Nullable String value) {
		return of(value, null);
	}

	@Contract("!null, _ -> !null")
	public static @Nullable Origin of(@Nullable String value, @Nullable Origin parent) {
		return (value != null) ? new MockOrigin(value, parent) : null;
	}

}
