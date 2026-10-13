/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		String foo = System.getProperty("foo");
		if (!"value 1".equals(foo)) {
			throw new IllegalStateException("foo system property mismatch (got [" + foo + "]");
		}
		String bar = System.getProperty("bar");
		if (!"value2".equals(bar)) {
			throw new IllegalStateException("bar system property mismatch (got [" + bar + "]");
		}
		String property1 = System.getProperty("property1");
		if (!"value1".equals(property1)) {
			throw new IllegalStateException("property1 system property mismatch (got [" + property1 + "]");
		}
		String property2 = System.getProperty("property2");
		if (!"".equals(property2)) {
			throw new IllegalStateException("property2 system property mismatch (got [" + property2 + "]");
		}
		String property3 = System.getProperty("property3");
		if (!"run-jvmargs".equals(property3)) {
			throw new IllegalStateException("property3 system property mismatch (got [" + property3 + "]");
		}
		System.out.println("I haz been run");
	}

}
