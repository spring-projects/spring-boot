/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		try {
			Class.forName("org.springframework.context.ApplicationContext");
			throw new IllegalStateException("Test dependencies should not have been added to classpath");
		}
		catch (ClassNotFoundException e) {
			// OK
		}
		try {
			Class.forName("org.test.SampleTest");
			throw new IllegalStateException("Test classes should not have been added to classpath");
		}
		catch (ClassNotFoundException e) {
			// OK
		}
		System.out.println("I haz been run");
	}

}
