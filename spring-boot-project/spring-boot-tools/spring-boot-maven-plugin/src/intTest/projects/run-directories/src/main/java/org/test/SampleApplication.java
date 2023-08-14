/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
