/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.deprecatedsince;

import org.springframework.boot.build.architecture.annotations.TestDeprecatedConfigurationProperty;

public class DeprecatedConfigurationPropertySince {

	private String property;

	@TestDeprecatedConfigurationProperty(reason = "no longer used")
	@Deprecated
	public String getProperty() {
		return this.property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

}
