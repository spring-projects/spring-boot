/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.tools.sample;

/**
 * Sample annotated class with a main method.
 *
 * @author Andy Wilkinson
 */
@SomeApplication
public class AnnotatedClassWithMainMethod {

	public void run() {
		System.out.println("Hello World");
	}

	public static void main(String[] args) {
		new AnnotatedClassWithMainMethod().run();
	}

}
