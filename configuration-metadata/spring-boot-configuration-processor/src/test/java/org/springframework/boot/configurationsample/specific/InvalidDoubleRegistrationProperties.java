/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Test that compilation fails if the same type is registered twice with the same prefix.
 *
 * @author Stephane Nicoll
 */
public class InvalidDoubleRegistrationProperties {

	@TestConfigurationProperties("foo")
	public Foo foo() {
		return new Foo();
	}

	@TestConfigurationProperties("foo")
	public static class Foo {

		private String name;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
