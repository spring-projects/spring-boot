/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationpropertiessource.hashset;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.build.architecture.annotations.TestConfigurationPropertiesSource;

/**
 * Test {@link TestConfigurationPropertiesSource} using {@link HashSet}.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationPropertiesSource
public class ConfigurationPropertiesWithHashSet {

	private Set<String> items = new HashSet<>();

	public Set<String> getItems() {
		return this.items;
	}

	public void setItems(Set<String> items) {
		this.items = items;
	}

}
