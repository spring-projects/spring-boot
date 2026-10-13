/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Sample with builder style setters.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("builder")
public class BuilderPojo {

	private String name;

	public String getName() {
		return this.name;
	}

	public BuilderPojo setName(String name) {
		this.name = name;
		return this;
	}

}
