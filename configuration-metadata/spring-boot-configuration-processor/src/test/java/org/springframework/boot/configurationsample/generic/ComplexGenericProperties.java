/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.generic;

import org.springframework.boot.configurationsample.TestConfigurationProperties;
import org.springframework.boot.configurationsample.TestNestedConfigurationProperty;

/**
 * More advanced generic setup.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("generic")
public class ComplexGenericProperties {

	@TestNestedConfigurationProperty
	private final UpperBoundGenericPojo<Test> test = new UpperBoundGenericPojo<>();

	public UpperBoundGenericPojo<Test> getTest() {
		return this.test;
	}

	public enum Test {

		ONE, TWO, THREE

	}

}
