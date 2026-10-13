/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Builder pattern with a resolved generic
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("builder")
public class ConcreteBuilderProperties extends GenericBuilderProperties<ConcreteBuilderProperties> {

	private String description;

	public String getDescription() {
		return this.description;
	}

	public ConcreteBuilderProperties setDescription(String description) {
		this.description = description;
		return this;
	}

}
