/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) {
		String workingDirectory = System.getProperty("user.dir");
		System.out.println(String.format("I haz been run from %s", workingDirectory));
	}

}
