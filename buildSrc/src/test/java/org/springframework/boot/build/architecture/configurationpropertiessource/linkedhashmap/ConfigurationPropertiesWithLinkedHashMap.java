/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationpropertiessource.linkedhashmap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.build.architecture.annotations.TestConfigurationPropertiesSource;

/**
 * Test {@link TestConfigurationPropertiesSource} using {@link LinkedHashMap}.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationPropertiesSource
public class ConfigurationPropertiesWithLinkedHashMap {

	private Map<String, String> properties = new LinkedHashMap<>();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
