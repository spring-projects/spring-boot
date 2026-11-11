/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source.generation;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.configurationsample.TestConfigurationPropertiesSource;

@Getter
@Setter
@TestConfigurationPropertiesSource
public class LombokPropertiesSource {

	/**
	 * Description of this simple property.
	 */
	@SuppressWarnings("unused")
	private String name = "boot";

	/**
	 * Whether it is enabled.
	 */
	@SuppressWarnings("unused")
	private boolean enabled;

}
