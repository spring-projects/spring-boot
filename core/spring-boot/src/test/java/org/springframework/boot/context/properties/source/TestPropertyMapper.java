/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Test {@link PropertyMapper} implementation.
 */
class TestPropertyMapper implements PropertyMapper {

	private final MultiValueMap<ConfigurationPropertyName, String> fromConfig = new LinkedMultiValueMap<>();

	private final Map<String, ConfigurationPropertyName> fromSource = new LinkedHashMap<>();

	void addFromPropertySource(String from, String to) {
		this.fromSource.put(from, ConfigurationPropertyName.of(to));
	}

	void addFromConfigurationProperty(ConfigurationPropertyName from, String... to) {
		for (String propertySourceName : to) {
			this.fromConfig.add(from, propertySourceName);
		}
	}

	@Override
	public List<String> map(ConfigurationPropertyName configurationPropertyName) {
		return this.fromConfig.getOrDefault(configurationPropertyName, Collections.emptyList());
	}

	@Override
	public ConfigurationPropertyName map(String propertySourceName) {
		return this.fromSource.getOrDefault(propertySourceName, ConfigurationPropertyName.EMPTY);
	}

}
