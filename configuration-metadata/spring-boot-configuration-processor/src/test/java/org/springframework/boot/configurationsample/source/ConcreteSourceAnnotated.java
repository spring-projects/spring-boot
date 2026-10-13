/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@TestConfigurationProperties("example")
public class ConcreteSourceAnnotated {

	@TestNestedConfigurationProperty
	private final ConcreteSource nested = new ConcreteSource();

	public ConcreteSource getNested() {
		return this.nested;
	}

}
