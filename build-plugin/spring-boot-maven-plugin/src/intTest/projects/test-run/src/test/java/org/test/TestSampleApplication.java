/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import java.io.File;
import java.lang.management.ManagementFactory;

public class TestSampleApplication {

	public static void main(String[] args) {
		System.out.println("Main class name = " + TestSampleApplication.class.getName());
		int i = 1;
		for (String entry : ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator)) {
			System.out.println(i++ + ". " + entry);
		}
	}

}
