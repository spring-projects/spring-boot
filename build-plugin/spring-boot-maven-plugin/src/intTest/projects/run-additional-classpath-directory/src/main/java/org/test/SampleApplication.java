/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class SampleApplication {

	public static void main(String[] args) {
		if (!readContent("one.txt").contains("1")) {
			throw new IllegalArgumentException("Invalid content for one.txt");
		}
		if (!readContent("another/two.txt").contains("2")) {
			throw new IllegalArgumentException("Invalid content for another/two.txt");
		}
		System.out.println("I haz been run");
	}

	private static String readContent(String location) {
		InputStream in = SampleApplication.class.getClassLoader().getResourceAsStream(location);
		if (in == null) {
			throw new IllegalArgumentException("Not found: '" + location + "'");
		}
		try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8)) {
			return scanner.useDelimiter("\\A").next();
		}
	}

}
