/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.bind;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link PropertyNamePatternsMatcher} that matches when a property name exactly matches
 * one of the given names, or starts with one of the given names followed by a delimiter.
 * This implementation is optimized for frequent calls.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class DefaultPropertyNamePatternsMatcher implements PropertyNamePatternsMatcher {

	private final char[] delimiters;

	private final boolean ignoreCase;

	private final String[] names;

	protected DefaultPropertyNamePatternsMatcher(char[] delimiters, String... names) {
		this(delimiters, false, names);
	}

	protected DefaultPropertyNamePatternsMatcher(char[] delimiters, boolean ignoreCase,
			String... names) {
		this(delimiters, ignoreCase, new HashSet<String>(Arrays.asList(names)));
	}

	DefaultPropertyNamePatternsMatcher(char[] delimiters, boolean ignoreCase,
			Set<String> names) {
		this.delimiters = delimiters;
		this.ignoreCase = ignoreCase;
		this.names = names.toArray(new String[names.size()]);
	}

	@Override
	public boolean matches(String propertyName) {
		char[] propertyNameChars = propertyName.toCharArray();
		boolean[] match = new boolean[this.names.length];
		boolean noneMatched = true;
		for (int i = 0; i < this.names.length; i++) {
			if (this.names[i].length() <= propertyNameChars.length) {
				match[i] = true;
				noneMatched = false;
			}
		}
		if (noneMatched) {
			return false;
		}
		for (int charIndex = 0; charIndex < propertyNameChars.length; charIndex++) {
			for (int nameIndex = 0; nameIndex < this.names.length; nameIndex++) {
				if (match[nameIndex]) {
					match[nameIndex] = false;
					if (charIndex < this.names[nameIndex].length()) {
						if (isCharMatch(this.names[nameIndex].charAt(charIndex),
								propertyNameChars[charIndex])) {
							match[nameIndex] = true;
							noneMatched = false;
						}
					}
					else {
						char charAfter = propertyNameChars[this.names[nameIndex]
								.length()];
						if (isDelimiter(charAfter)) {
							match[nameIndex] = true;
							noneMatched = false;
						}
					}
				}
			}
			if (noneMatched) {
				return false;
			}
		}
		for (int i = 0; i < match.length; i++) {
			if (match[i]) {
				return true;
			}
		}
		return false;
	}

	private boolean isCharMatch(char c1, char c2) {
		if (this.ignoreCase) {
			return Character.toLowerCase(c1) == Character.toLowerCase(c2);
		}
		return c1 == c2;
	}

	private boolean isDelimiter(char c) {
		for (char delimiter : this.delimiters) {
			if (c == delimiter) {
				return true;
			}
		}
		return false;
	}

}
