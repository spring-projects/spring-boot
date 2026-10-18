/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

/**
 * Utility used to check names are valid before performing expensive loading operations.
 *
 * @author Phillip Webb
 */
final class LoadableNames {

	private LoadableNames() {
	}

	static boolean isValidClassName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		boolean segmentStart = true;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			boolean separator = (ch == '.' || ch == '/');
			// JVM class-file names are not restricted to Java identifiers.
			// ';' ends an object type descriptor and '[' starts an array type descriptor,
			// so they are not valid in a class name.
			if ((separator && segmentStart) || ch == ';' || ch == '[') {
				return false;
			}
			segmentStart = separator;
		}
		return !segmentStart;
	}

	static boolean isValidResourceName(String name) {
		return (name != null && !name.isEmpty() && name.indexOf(':') == -1);
	}

}
