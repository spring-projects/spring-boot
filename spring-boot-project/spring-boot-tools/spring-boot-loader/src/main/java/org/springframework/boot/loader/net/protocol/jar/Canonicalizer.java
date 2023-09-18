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

package org.springframework.boot.loader.net.protocol.jar;

/**
 * Internal utility used by the {@link Handler} to canonicalize paths. This implementation
 * should behave the same as the canonicalization functions in
 * {@code sun.net.www.protocol.jar.Handler}.
 *
 * @author Phillip Webb
 */
final class Canonicalizer {

	private Canonicalizer() {
	}

	static String canonicalizeAfter(String path, int pos) {
		int pathLength = path.length();
		boolean noDotSlash = path.indexOf("./", pos) == -1;
		if (pos >= pathLength || (noDotSlash && path.charAt(pathLength - 1) != '.')) {
			return path;
		}
		String before = path.substring(0, pos);
		String after = path.substring(pos);
		return before + canonicalize(after);
	}

	static String canonicalize(String path) {
		path = removeEmbeddedSlashDotDotSlash(path);
		path = removedEmbdeddedSlashDotSlash(path);
		path = removeTrailingSlashDotDot(path);
		path = removeTrailingSlashDot(path);
		return path;
	}

	private static String removeEmbeddedSlashDotDotSlash(String path) {
		int index;
		while ((index = path.indexOf("/../")) >= 0) {
			int priorSlash = path.lastIndexOf('/', index - 1);
			String after = path.substring(index + 3);
			path = (priorSlash >= 0) ? path.substring(0, priorSlash) + after : after;
		}
		return path;
	}

	private static String removedEmbdeddedSlashDotSlash(String path) {
		int index;
		while ((index = path.indexOf("/./")) >= 0) {
			String before = path.substring(0, index);
			String after = path.substring(index + 2);
			path = before + after;
		}
		return path;
	}

	private static String removeTrailingSlashDot(String path) {
		return (!path.endsWith("/.")) ? path : path.substring(0, path.length() - 1);
	}

	private static String removeTrailingSlashDotDot(String path) {
		int index;
		while (path.endsWith("/..")) {
			index = path.indexOf("/..");
			int priorSlash = path.lastIndexOf('/', index - 1);
			path = (priorSlash >= 0) ? path.substring(0, priorSlash + 1) : path.substring(0, index);
		}
		return path;
	}

}
