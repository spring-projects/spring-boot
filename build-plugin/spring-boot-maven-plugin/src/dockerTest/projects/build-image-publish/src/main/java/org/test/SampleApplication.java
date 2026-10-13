/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

public class SampleApplication {

	public static void main(String[] args) throws Exception {
		System.out.println("Launched");
		synchronized(args) {
			args.wait(); // Prevent exit"
		}
	}

}
