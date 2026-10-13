/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.hashmap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

/**
 * Test {@link TestConfigurationProperties} using {@link HashMap}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 */
@TestConfigurationProperties("testing")
public class ConfigurationPropertiesWithHashMap {

	private Map<String, String> properties = new HashMap<>();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
