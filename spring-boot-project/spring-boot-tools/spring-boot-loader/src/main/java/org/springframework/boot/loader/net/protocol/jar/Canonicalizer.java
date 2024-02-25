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

	/**
     * Constructs a new instance of the Canonicalizer class.
     */
    private Canonicalizer() {
	}

	/**
     * This method takes a path and a position as input and returns the canonicalized path after the given position.
     * 
     * @param path The original path to be canonicalized.
     * @param pos The position in the path after which the canonicalization should be performed.
     * @return The canonicalized path after the given position.
     */
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

	/**
     * This method takes a path as input and performs canonicalization on it.
     * 
     * @param path the path to be canonicalized
     * @return the canonicalized path
     */
    static String canonicalize(String path) {
		path = removeEmbeddedSlashDotDotSlash(path);
		path = removeEmbeddedSlashDotSlash(path);
		path = removeTrailingSlashDotDot(path);
		path = removeTrailingSlashDot(path);
		return path;
	}

	/**
     * Removes any occurrences of "/../" in the given path string.
     * 
     * @param path the path string to be processed
     * @return the path string with "/../" occurrences removed
     */
    private static String removeEmbeddedSlashDotDotSlash(String path) {
		int index;
		while ((index = path.indexOf("/../")) >= 0) {
			int priorSlash = path.lastIndexOf('/', index - 1);
			String after = path.substring(index + 3);
			path = (priorSlash >= 0) ? path.substring(0, priorSlash) + after : after;
		}
		return path;
	}

	/**
     * Removes any occurrences of "/./" in the given path.
     * 
     * @param path the path to be processed
     * @return the path with "/./" occurrences removed
     */
    private static String removeEmbeddedSlashDotSlash(String path) {
		int index;
		while ((index = path.indexOf("/./")) >= 0) {
			String before = path.substring(0, index);
			String after = path.substring(index + 2);
			path = before + after;
		}
		return path;
	}

	/**
     * Removes the trailing "/." from the given path if it exists.
     * 
     * @param path the path to be processed
     * @return the path without the trailing "/." if it exists, otherwise the original path
     */
    private static String removeTrailingSlashDot(String path) {
		return (!path.endsWith("/.")) ? path : path.substring(0, path.length() - 1);
	}

	/**
     * Removes trailing "/.." segments from a given path.
     * 
     * @param path the path to be processed
     * @return the path with trailing "/.." segments removed
     */
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
