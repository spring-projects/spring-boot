/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source.generation;

import org.springframework.boot.configurationsample.TestConfigurationPropertiesSource;

@TestConfigurationPropertiesSource
public class NestedPropertiesSource {

	/**
	 * A name.
	 */
	private String name;

	private final Nested nested = new Nested();

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Nested getNested() {
		return this.nested;
	}

	public static class Nested {

		/**
		 * Another name.
		 */
		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
