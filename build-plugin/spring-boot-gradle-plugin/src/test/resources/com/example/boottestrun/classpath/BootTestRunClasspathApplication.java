/*
 * Copyright 2012-present the original author or authors.
 */

package com.example.boottestrun.classpath;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * Application used for testing {@code bootTestRun}'s classpath handling.
 *
 * @author Andy Wilkinson
 */
public class BootTestRunClasspathApplication {

	protected BootTestRunClasspathApplication() {

	}

	public static void main(String[] args) {
		System.out.println("Main class name = " + BootTestRunClasspathApplication.class.getName());
		int i = 1;
		for (String entry : ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator)) {
			System.out.println(i++ + ". " + entry);
		}
	}

}
