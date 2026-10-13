/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * A property that is exposed by static accessors.
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("specific")
public class StaticAccessor {

	private static String name;

	private int counter = 42;

	public static String getName() {
		return name;
	}

	public static void setName(String name) {
		StaticAccessor.name = name;
	}

	public int getCounter() {
		return this.counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

}
