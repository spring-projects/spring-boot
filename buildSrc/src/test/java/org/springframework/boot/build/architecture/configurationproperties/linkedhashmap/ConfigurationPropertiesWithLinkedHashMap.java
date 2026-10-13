/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.linkedhashmap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

/**
 * Test {@link TestConfigurationProperties} using {@link LinkedHashMap}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 */
@TestConfigurationProperties("testing")
public class ConfigurationPropertiesWithLinkedHashMap {

	private Map<String, String> properties = new LinkedHashMap<>();

	public Map<String, String> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
