/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

/**
 * Utility used to check names are valid before performing expensive loading operations.
 *
 * @author Phillip Webb
 * @author Jan Mohr
 */
final class LoadableNames {

	private static final char ESCAPE = '\\';

	private LoadableNames() {
	}

	/**
	 * Return true of the class name is valid according to <a href=
	 * "https://docs.oracle.com/javase/specs/jvms/se14/html/jvms-4.html#jvms-4.2">Section
	 * 4.2 of the Java Virtual Machine Specification</a>.
	 * @param name the name to test
	 * @return if the name is valid
	 */
	static boolean isValidClassName(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}
		boolean escape = false;
		boolean validPackagePart = true;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (isRestrictedNameCharacter(ch)) {
				return false;
			}
			if (isPartSeparator(ch)) {
				if (!validPackagePart || escape || i == 0 || i == name.length() - 1
						|| isPartSeparator(name.charAt(i + 1))) {
					return false;
				}
				validPackagePart = true;
				continue;
			}
			validPackagePart = validPackagePart && !isInRestrictedPackageCharacterRange(ch);
			if (escape) {
				validPackagePart = validPackagePart && (ch == ESCAPE || isPackageCharacterThatShouldBeEscaped(ch));
				escape = false;
				continue;
			}
			if (ch == ESCAPE) {
				escape = true;
				continue;
			}
			validPackagePart = validPackagePart && !isPackageCharacterThatShouldBeEscaped(ch);
		}
		return true;
	}

	private static boolean isRestrictedNameCharacter(char ch) {
		// JVMS 4.2.1. Binary Class and Interface Names
		return ch == ';' || ch == '[' || ch == '<' || ch == '>';
	}

	private static boolean isInRestrictedPackageCharacterRange(char ch) {
		// JVMS 4.2.3. Module and Package Names
		return ch >= 0 && ch <= 0x1F;
	}

	private static boolean isPackageCharacterThatShouldBeEscaped(char ch) {
		// JVMS 4.2.3. Module and Package Names
		return ch == ':' || ch == '@';
	}

	private static boolean isPartSeparator(char ch) {
		return ch == '.' || ch == '/';
	}

}
