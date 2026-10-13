/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.lombok;

import lombok.Data;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

/**
 * Demonstrate the auto-detection of inner config classes using Lombok.
 *
 * @author Stephane Nicoll
 */
@Data
@TestConfigurationProperties("config")
@SuppressWarnings("unused")
public class LombokInnerClassProperties {

	private final Foo first = new Foo();

	private Foo second = new Foo();

	@TestNestedConfigurationProperty
	private final SimpleLombokPojo third = new SimpleLombokPojo();

	private Fourth fourth;

	// Only there to record the source method
	public SimpleLombokPojo getThird() {
		return this.third;
	}

	@Data
	public static class Foo {

		private String name;

		private final Bar bar = new Bar();

		@Data
		public static class Bar {

			private String name;

		}

	}

	public enum Fourth {

		YES, NO

	}

}
