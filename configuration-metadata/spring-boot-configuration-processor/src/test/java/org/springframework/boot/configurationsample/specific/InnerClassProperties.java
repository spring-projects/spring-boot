/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

/**
 * Demonstrate the auto-detection of inner config classes.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("config")
public class InnerClassProperties {

	private final Foo first = new Foo();

	private Foo second = new Foo();

	@TestNestedConfigurationProperty
	private final SimplePojo third = new SimplePojo();

	private Fourth fourth;

	private final DeprecatedSimplePojo fifth = new DeprecatedSimplePojo();

	public Foo getFirst() {
		return this.first;
	}

	public Foo getTheSecond() {
		return this.second;
	}

	public void setTheSecond(Foo second) {
		this.second = second;
	}

	public SimplePojo getThird() {
		return this.third;
	}

	public Fourth getFourth() {
		return this.fourth;
	}

	public void setFourth(Fourth fourth) {
		this.fourth = fourth;
	}

	@TestNestedConfigurationProperty
	public DeprecatedSimplePojo getFifth() {
		return this.fifth;
	}

	public static class Foo {

		private String name;

		private final Bar bar = new Bar();

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Bar getBar() {
			return this.bar;
		}

		public static class Bar {

			private String name;

			public String getName() {
				return this.name;
			}

			public void setName(String name) {
				this.name = name;
			}

		}

	}

	public enum Fourth {

		YES, NO

	}

}
