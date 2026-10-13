/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample for testing method configuration that uses a constructor that should not be
 * associated to constructor binding.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class SingleConstructorMethodConfig {

	@TestConfigurationProperties("foo")
	public Foo foo() {
		return new Foo(new Object());
	}

	public static class Foo {

		private String name;

		private boolean flag;

		private final Object myService;

		public Foo(Object myService) {
			this.myService = myService;
		}

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
