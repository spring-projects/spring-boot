/*
 * Copyright 2012-present the original author or authors.
 */

package com.example.bootjar.classpath;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Application used for testing classpath handling with BootJar.
 *
 * @author Andy Wilkinson
 */
public class BootJarClasspathApplication {

	protected BootJarClasspathApplication() {

	}

	public static void main(String[] args) {
		int i = 1;
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (URL url : ((URLClassLoader) classLoader).getURLs()) {
			System.out.println(i++ + ". " + url.getFile());
		}
	}

}
