/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import java.io.File;

public class SampleApplication {

	public static void main(String[] args) {
		System.out.println("I haz been run from '" + new File("").getAbsolutePath() + "'");
	}

}
