/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Inner properties, in immutable format.
 *
 * @author Phillip Webb
 */
@TestConfigurationProperties("test")
public class DeducedImmutableClassProperties {

	private final Nested nested;

	public DeducedImmutableClassProperties(@TestDefaultValue Nested nested) {
		this.nested = nested;
	}

	public Nested getNested() {
		return this.nested;
	}

	public static class Nested {

		private final String name;

		public Nested(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

	}

}
