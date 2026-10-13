/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.inheritance;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

public class ChildPropertiesConfig {

	@TestConfigurationProperties("inheritance")
	public ChildProperties childConfig() {
		return new ChildProperties();
	}

}
