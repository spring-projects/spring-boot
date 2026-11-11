/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.classprefixonly;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

@TestConfigurationProperties(prefix = "testing")
public class ConfigurationPropertiesWithPrefixOnly {

	private String property;

	public String getProperty() {
		return this.property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

}
