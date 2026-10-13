/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.enummap;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

/**
 * Test {@link TestConfigurationProperties} using {@link EnumMap}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 */
@TestConfigurationProperties("testing")
public class ConfigurationPropertiesWithEnumMap {

	private Map<Example, String> properties = new EnumMap<>(Example.class);

	public Map<Example, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<Example, String> properties) {
		this.properties = properties;
	}

	enum Example {

		ONE

	}

}
