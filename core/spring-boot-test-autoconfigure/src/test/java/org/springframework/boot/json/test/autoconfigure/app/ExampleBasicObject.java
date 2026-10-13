/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.json.test.autoconfigure.app;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Example object to read/write as JSON.
 *
 * @author Phillip Webb
 */
public class ExampleBasicObject {

	private @Nullable String value;

	public @Nullable String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == getClass()) {
			return Objects.equals(this.value, ((ExampleBasicObject) obj).value);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.value);
	}

}
