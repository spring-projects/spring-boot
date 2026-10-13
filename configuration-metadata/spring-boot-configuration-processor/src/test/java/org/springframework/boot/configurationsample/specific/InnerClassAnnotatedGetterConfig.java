/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Demonstrate that a method that exposes a root group within an annotated class is
 * ignored as it should.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("specific")
public class InnerClassAnnotatedGetterConfig {

	private String value;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@TestConfigurationProperties("foo")
	public Foo getFoo() {
		return new Foo();
	}

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
