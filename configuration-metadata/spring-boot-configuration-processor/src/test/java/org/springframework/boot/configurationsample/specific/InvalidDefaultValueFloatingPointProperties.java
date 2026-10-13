/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Demonstrates that an invalid default floating point value leads to a compilation
 * failure.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("test")
public class InvalidDefaultValueFloatingPointProperties {

	private final Double ratio;

	public InvalidDefaultValueFloatingPointProperties(@TestDefaultValue("55.55.33") Double ratio) {
		this.ratio = ratio;
	}

	public Double getRatio() {
		return this.ratio;
	}

}
