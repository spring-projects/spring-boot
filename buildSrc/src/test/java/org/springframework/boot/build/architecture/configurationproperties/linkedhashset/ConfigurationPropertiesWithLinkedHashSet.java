/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.build.architecture.configurationproperties.linkedhashset;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.build.architecture.annotations.TestConfigurationProperties;

/**
 * Test {@link TestConfigurationProperties} using {@link LinkedHashSet}.
 *
 * @author Venkata Naga Sai Srikanth Gollapudi
 */
@TestConfigurationProperties("testing")
public class ConfigurationPropertiesWithLinkedHashSet {

	private Set<String> items = new LinkedHashSet<>();

	public Set<String> getItems() {
		return this.items;
	}

	public void setItems(Set<String> items) {
		this.items = items;
	}

}
