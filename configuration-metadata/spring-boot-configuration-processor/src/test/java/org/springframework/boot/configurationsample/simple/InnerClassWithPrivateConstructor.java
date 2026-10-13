/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Nested properties with a private constructor.
 *
 * @author Phillip Webb
 */
@TestConfigurationProperties("config")
public class InnerClassWithPrivateConstructor {

	private Nested nested = new Nested("whatever");

	public Nested getNested() {
		return this.nested;
	}

	public void setNested(Nested nested) {
		this.nested = nested;
	}

	public static final class Nested {

		private String name;

		private Nested(String ignored) {
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
