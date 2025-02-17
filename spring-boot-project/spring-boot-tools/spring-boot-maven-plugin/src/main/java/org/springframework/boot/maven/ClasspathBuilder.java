/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class to build the -cp (classpath) argument of a java process.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 */
final class ClasspathBuilder {

	private ClasspathBuilder() {
	}

	/**
	 * Builds a classpath string or an argument file representing the classpath, depending
	 * on the operating system.
	 * @param urls an array of {@link URL} representing the elements of the classpath
	 * @return the classpath; on Windows, the path to an argument file is returned,
	 * prefixed with '@'
	 */
	static String build(URL... urls) {
		if (ObjectUtils.isEmpty(urls)) {
			return "";
		}
		if (urls.length == 1) {
			return toFile(urls[0]).toString();
		}
		StringBuilder builder = new StringBuilder();
		for (URL url : urls) {
			if (!builder.isEmpty()) {
				builder.append(File.pathSeparator);
			}
			builder.append(toFile(url));
		}
		String classpath = builder.toString();
		if (runsOnWindows()) {
			try {
				return "@" + ArgFile.create(classpath);
			}
			catch (IOException ex) {
				return classpath;
			}
		}
		return classpath;
	}

	private static File toFile(URL url) {
		try {
			return new File(url.toURI());
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	private static boolean runsOnWindows() {
		String os = System.getProperty("os.name");
		if (!StringUtils.hasText(os)) {
			return false;
		}
		return os.toLowerCase(Locale.ROOT).contains("win");
	}

}
