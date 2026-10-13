/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		if (isClassPresent("org.apache.log4j.Logger")) {
			throw new IllegalStateException("Log4j was present and should not");
		}
		if (isClassPresent("jakarta.servlet.Servlet")) {
			throw new IllegalStateException("servlet-api was present and should not");
		}
		System.out.println("I haz been run");
	}

	private static boolean isClassPresent(String className) {

		try {
			ClassLoader classLoader = SampleApplication.class.getClassLoader();
			Class.forName(className, false, classLoader);
			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}

}
