/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestConstructorBinding;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Demonstrates that an empty default value on a property leads to no default value.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("test")
public class EmptyDefaultValueProperties {

	private final String name;

	@TestConstructorBinding
	public EmptyDefaultValueProperties(@TestDefaultValue String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}
