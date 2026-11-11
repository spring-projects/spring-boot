/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationpropertiessource.enummap;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.build.architecture.annotations.TestConfigurationPropertiesSource;

/**
 * Test {@link TestConfigurationPropertiesSource} using {@link EnumMap}.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationPropertiesSource
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
