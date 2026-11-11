/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven.sample;

/**
 * Sample class with a main method.
 *
 * @author Phillip Webb
 */
public class ClassWithMainMethod {

	public void run() {
		System.out.println("Hello World");
	}

	public static void main(String[] args) {
		new ClassWithMainMethod().run();
	}

}
