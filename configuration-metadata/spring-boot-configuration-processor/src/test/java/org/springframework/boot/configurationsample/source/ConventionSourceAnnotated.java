/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@TestConfigurationProperties(prefix = "example")
public class ConventionSourceAnnotated {

	@TestNestedConfigurationProperty
	private final ConventionSource nested = new ConventionSource();

	public ConventionSource getNested() {
		return this.nested;
	}

}
