/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) {

		Class<?> appContext = null;
		try {
			appContext = Class.forName("org.springframework.context.ApplicationContext");
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Test dependencies not added to classpath", e);
		}
		System.out.println("I haz been run");
	}

}
