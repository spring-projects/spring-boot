/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample for testing method configuration with deprecated class.
 *
 * @author Stephane Nicoll
 * @deprecated deprecated
 */
@Deprecated
public class DeprecatedClassMethodConfig {

	@TestConfigurationProperties("foo")
	public Foo foo() {
		return new Foo();
	}

	public static class Foo {

		private String name;

		private boolean flag;

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isFlag() {
			return this.flag;
		}

		public void setFlag(boolean flag) {
			this.flag = flag;
		}

	}

}
