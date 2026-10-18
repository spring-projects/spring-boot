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

	private static final String PACKAGE_INFO = "package-info";

	private LoadableNames() {
	}

	static boolean isValidClassName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		boolean identifierStart = true;
		for (int i = 0; i < name.length(); i++) {
			if (identifierStart && isFinalPackageInfoSegment(name, i)) {
				return true;
			}
			char ch = name.charAt(i);
			boolean dotOrSlash = (ch == '.' || ch == '/');
			if ((identifierStart && !Character.isJavaIdentifierStart(ch))
					|| !identifierStart && !Character.isJavaIdentifierPart(ch) && !dotOrSlash) {
				return false;
			}
			identifierStart = dotOrSlash;
		}
		return true;
	}

	private static boolean isFinalPackageInfoSegment(String name, int index) {
		return name.length() - index == PACKAGE_INFO.length() && name.startsWith(PACKAGE_INFO, index);
	}

	static boolean isValidResourceName(String name) {
		return (name != null && !name.isEmpty() && name.indexOf(':') == -1);
	}

}
