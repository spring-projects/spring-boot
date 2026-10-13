/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample for testing invalid method configuration.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("something")
public class InvalidMethodConfig {

	private String name;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@TestConfigurationProperties("invalid")
	InvalidMethodConfig foo() {
		return new InvalidMethodConfig();
	}

}
