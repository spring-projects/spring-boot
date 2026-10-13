/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import java.util.Arrays;

public class SampleApplication {

	public static void main(String[] args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("Missing arguments " + Arrays.toString(args));
		}
		if (!args[0].startsWith("--management.endpoints.web.exposure.include=")) {
			throw new IllegalArgumentException("Invalid argument " + args[0]);
		}
		if (!args[1].startsWith("--spring.profiles.active=")) {
			throw new IllegalArgumentException("Invalid argument " + args[1]);
		}
		String endpoints = args[0].split("=")[1];
		String profile = args[1].split("=")[1];
		System.out.println("I haz been run with profile(s) '" + profile + "' and endpoint(s) '" + endpoints + "'");
	}

}
