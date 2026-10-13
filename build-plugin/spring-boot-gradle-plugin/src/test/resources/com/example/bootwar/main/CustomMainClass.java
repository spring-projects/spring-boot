/*
 * Copyright 2012-present the original author or authors.
 */

package com.example.bootwar.main;

/**
 * Application used for testing {@code BootRun}'s main class configuration.
 *
 * @author Andy Wilkinson
 */
public class CustomMainClass {

	protected CustomMainClass() {

	}

	public static void main(String[] args) {
		System.out.println(CustomMainClass.class.getName());
	}

}
