/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source.generation;

import org.springframework.boot.configurationsample.TestConfigurationPropertiesSource;
import org.springframework.boot.configurationsample.TestDefaultValue;

@TestConfigurationPropertiesSource
public class ImmutablePropertiesSource {

	/**
	 * Description of this simple property.
	 */
	@SuppressWarnings("unused")
	private final String name;

	/**
	 * Whether it is enabled.
	 */
	@SuppressWarnings("unused")
	private final boolean enabled;

	public ImmutablePropertiesSource(@TestDefaultValue("boot") String name, boolean enabled) {
		this.name = name;
		this.enabled = enabled;
	}

}
