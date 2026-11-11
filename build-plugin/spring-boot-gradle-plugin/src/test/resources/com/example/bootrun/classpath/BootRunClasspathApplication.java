/*
 * Copyright 2012-present the original author or authors.
 */

package com.example.bootrun.classpath;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * Application used for testing {@code BootRun}'s classpath handling.
 *
 * @author Andy Wilkinson
 */
public class BootRunClasspathApplication {

	protected BootRunClasspathApplication() {

	}

	public static void main(String[] args) {
		System.out.println("Main class name = " + BootRunClasspathApplication.class.getName());
		int i = 1;
		for (String entry : ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator)) {
			System.out.println(i++ + ". " + entry);
		}
	}

}
