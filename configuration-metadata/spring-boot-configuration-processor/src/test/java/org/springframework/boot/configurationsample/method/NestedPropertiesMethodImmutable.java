/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.method;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

@TestConfigurationProperties("immutable-nested")
public class NestedPropertiesMethodImmutable {

	private final NestedProperty nested;

	NestedPropertiesMethodImmutable(NestedProperty nested) {
		this.nested = nested;

	}

	@TestNestedConfigurationProperty
	public NestedProperty getNested() {
		return this.nested;
	}

}
