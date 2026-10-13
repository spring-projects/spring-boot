/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.hashset;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

/**
 * Test {@link TestConfigurationProperties} using {@link HashSet}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 */
@TestConfigurationProperties("testing")
public class ConfigurationPropertiesWithHashSet {

	private Set<String> items = new HashSet<>();

	public Set<String> getItems() {
		return this.items;
	}

	public void setItems(Set<String> items) {
		this.items = items;
	}

}
