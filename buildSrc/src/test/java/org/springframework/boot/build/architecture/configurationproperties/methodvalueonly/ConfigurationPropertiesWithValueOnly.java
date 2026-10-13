/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.methodvalueonly;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

public class ConfigurationPropertiesWithValueOnly {

	private String property;

	@TestConfigurationProperties("testing")
	public String getProperty() {
		return this.property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

}
