/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample for testing mixed method and class configuration.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("conflict")
public class MethodAndClassConfig {

	private String value;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@TestConfigurationProperties("conflict")
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
