/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.inheritance;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

public class OverrideChildPropertiesConfig {

	@TestConfigurationProperties("inheritance")
	public OverrideChildProperties overrideChildProperties() {
		return new OverrideChildProperties();
	}

}
