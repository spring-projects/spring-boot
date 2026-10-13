/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.simple;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Configuration properties with a single deprecated element.
 *
 * @author Andy Wilkinson
 */
@TestConfigurationProperties("singlefielddeprecated")
public class DeprecatedFieldSingleProperty {

	@Deprecated
	private String name;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
