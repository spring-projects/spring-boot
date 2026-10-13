/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Demonstrates that an invalid default number value leads to a compilation failure.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("test")
public class InvalidDefaultValueNumberProperties {

	private final int counter;

	public InvalidDefaultValueNumberProperties(@TestDefaultValue("invalid") int counter) {
		this.counter = counter;
	}

	public int getCounter() {
		return this.counter;
	}

}
