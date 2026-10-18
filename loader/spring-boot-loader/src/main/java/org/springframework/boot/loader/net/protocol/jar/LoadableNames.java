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
		boolean identifierStart = true;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			boolean dotOrSlash = (ch == '.' || ch == '/');
			if ((identifierStart && dotOrSlash) || ch == ';' || ch == '[') {
				return false;
			}
			identifierStart = dotOrSlash;
		}
		return true;
	}

	static boolean isValidResourceName(String name) {
		return (name != null && !name.isEmpty() && name.indexOf(':') == -1);
	}

}
