/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import org.test.SampleLibrary;

public class SampleApplication {

	public static void main(String[] args) throws Exception {
		System.out.println(SampleLibrary.getMessage());
		synchronized (args) {
			args.wait(); // Prevent exit"
		}
	}

}
