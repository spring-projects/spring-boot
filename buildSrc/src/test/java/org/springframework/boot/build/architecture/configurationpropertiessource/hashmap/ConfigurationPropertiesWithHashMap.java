/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationpropertiessource.hashmap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.build.architecture.annotations.TestConfigurationPropertiesSource;

/**
 * Test {@link TestConfigurationPropertiesSource} using {@link HashMap}.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationPropertiesSource
public class ConfigurationPropertiesWithHashMap {

	private Map<String, String> properties = new HashMap<>();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
