/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.origin;

import java.net.URI;

/**
 * Simple class that understands Jar URLs can can provide short descriptions.
 *
 * @author Phillip Webb
 */
final class JarUri {

	private static final String JAR_SCHEME = "jar:";

	private static final String JAR_EXTENSION = ".jar";

	private final String uri;

	private final String description;

	private JarUri(String uri) {
		this.uri = uri;
		this.description = extractDescription(uri);
	}

	private String extractDescription(String uri) {
		uri = uri.substring(JAR_SCHEME.length());
		int firstDotJar = uri.indexOf(JAR_EXTENSION);
		String firstJar = getFilename(uri.substring(0, firstDotJar + JAR_EXTENSION.length()));
		uri = uri.substring(firstDotJar + JAR_EXTENSION.length());
		int lastDotJar = uri.lastIndexOf(JAR_EXTENSION);
		if (lastDotJar == -1) {
			return firstJar;
		}
		return firstJar + uri.substring(0, lastDotJar + JAR_EXTENSION.length());
	}

	private String getFilename(String string) {
		int lastSlash = string.lastIndexOf('/');
		return (lastSlash == -1) ? string : string.substring(lastSlash + 1);
	}

	String getDescription() {
		return this.description;
	}

	String getDescription(String existing) {
		return existing + " from " + this.description;
	}

	@Override
	public String toString() {
		return this.uri;
	}

	static JarUri from(URI uri) {
		return from(uri.toString());
	}

	static JarUri from(String uri) {
		if (uri.startsWith(JAR_SCHEME) && uri.contains(JAR_EXTENSION)) {
			return new JarUri(uri);
		}
		return null;
	}

}
