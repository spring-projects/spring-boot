/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.methodprefixonly;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

public class ConfigurationPropertiesWithPrefixOnly {

	private String property;

	@TestConfigurationProperties(prefix = "testing")
	public String getProperty() {
		return this.property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

}
