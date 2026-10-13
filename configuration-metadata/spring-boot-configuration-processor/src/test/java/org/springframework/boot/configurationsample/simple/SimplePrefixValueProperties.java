/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Properties with a simple prefix.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("simple")
public class SimplePrefixValueProperties {

	private String name;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
