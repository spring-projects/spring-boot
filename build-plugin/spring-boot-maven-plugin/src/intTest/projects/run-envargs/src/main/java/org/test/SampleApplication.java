/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		assertEnvValue("ENV1", "5000");
		assertEnvValue("ENV2", "Some Text");
		assertEnvValue("ENV3", "");
		assertEnvValue("ENV4", "");

		System.out.println("I haz been run");
	}

	private static void assertEnvValue(String envKey, String expectedValue) {
		String actual = System.getenv(envKey);
		if (!expectedValue.equals(actual)) {
			throw new IllegalStateException("env property [" + envKey + "] mismatch "
					+ "(got [" + actual + "], expected [" + expectedValue + "]");
		}
	}

}
