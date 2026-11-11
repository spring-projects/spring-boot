/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source.generation;

import org.springframework.boot.configurationsample.TestConfigurationPropertiesSource;

@TestConfigurationPropertiesSource
public class SimplePropertiesSource {

	/**
	 * Description of this simple property.
	 */
	private String name = "boot";

	/**
	 * Whether it is enabled.
	 */
	private boolean enabled;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
