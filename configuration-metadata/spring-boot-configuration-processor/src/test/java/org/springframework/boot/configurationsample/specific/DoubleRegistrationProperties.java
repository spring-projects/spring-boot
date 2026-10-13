/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * Test that the same type can be registered several times if the prefix is different.
 *
 * @author Stephane Nicoll
 */
public class DoubleRegistrationProperties {

	@TestConfigurationProperties("one")
	public SimplePojo one() {
		return new SimplePojo();
	}

	@TestConfigurationProperties("two")
	public SimplePojo two() {
		return new SimplePojo();
	}

}
