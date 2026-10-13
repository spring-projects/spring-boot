/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import java.util.Arrays;

public class SampleApplication {

	public static void main(String[] args) {
		if (args.length < 1) {
			throw new IllegalArgumentException("Missing active profile argument " + Arrays.toString(args));
		}
		String argument = args[0];
		if (!argument.startsWith("--spring.profiles.active=")) {
			throw new IllegalArgumentException("Invalid argument " + argument);
		}
		int index = args[0].indexOf('=');
		String profile = argument.substring(index + 1);
		System.out.println("I haz been run with profile(s) '" + profile + "'");
	}

}
