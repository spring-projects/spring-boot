/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;
import org.springframework.boot.configurationsample.specific.SimplePojo;

/**
 * Inner properties, in immutable format.
 *
 * @author Stephane Nicoll
 */
public class ImmutableInnerClassProperties {

	private final Foo first;

	private Foo second;

	@TestNestedConfigurationProperty
	private final SimplePojo third;

	private final Fourth fourth;

	public ImmutableInnerClassProperties(Foo first, Foo second, SimplePojo third, Fourth fourth) {
		this.first = first;
		this.second = second;
		this.third = third;
		this.fourth = fourth;
	}

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
