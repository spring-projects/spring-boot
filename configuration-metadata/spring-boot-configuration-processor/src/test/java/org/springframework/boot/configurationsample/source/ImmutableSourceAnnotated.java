/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.source;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@TestConfigurationProperties("example")
public class ImmutableSourceAnnotated {

	@TestNestedConfigurationProperty
	private final ImmutableSource nested;

	public ImmutableSourceAnnotated(ImmutableSource nested) {
		this.nested = nested;
	}

}
