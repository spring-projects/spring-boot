/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.classprefixandignore;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

@TestConfigurationProperties(prefix = "testing", ignoreUnknownFields = false)
public class ConfigurationPropertiesWithPrefixAndIgnore {

	private String property;

	public String getProperty() {
		return this.property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

}
