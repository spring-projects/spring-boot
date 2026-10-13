/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample for testing method configuration.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("something")
public class EmptyTypeMethodConfig {

	private String name;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@TestConfigurationProperties("something")
	public Foo foo() {
		return new Foo();
	}

	static class Foo {

	}

}
