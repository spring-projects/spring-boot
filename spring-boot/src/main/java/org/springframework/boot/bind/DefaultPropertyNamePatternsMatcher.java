/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Default {@link PropertyNamePatternsMatcher} that matches when a property name exactly
 * matches one of the given names, or starts with one of the given names followed by '.'
 * or '_'. This implementation is optimized for frequent calls.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
class DefaultPropertyNamePatternsMatcher implements PropertyNamePatternsMatcher {

	private final String[] names;

	public DefaultPropertyNamePatternsMatcher(String... names) {
		this(new HashSet<String>(Arrays.asList(names)));
	}

	public DefaultPropertyNamePatternsMatcher(Set<String> names) {
		this.names = names.toArray(new String[names.size()]);
	}

	@Override
	public boolean matches(String propertyName) {
		char[] propertNameChars = propertyName.toCharArray();
		boolean[] match = new boolean[this.names.length];
		boolean noneMatched = true;
		for (int i = 0; i < this.names.length; i++) {
			if (this.names[i].length() <= propertNameChars.length) {
				match[i] = true;
				noneMatched = false;
			}
		}
		if (noneMatched) {
			return false;
		}
		for (int charIndex = 0; charIndex < propertNameChars.length; charIndex++) {
			noneMatched = true;
			for (int nameIndex = 0; nameIndex < this.names.length; nameIndex++) {
				if (match[nameIndex]) {
					if (charIndex < this.names[nameIndex].length()) {
						if (this.names[nameIndex].charAt(charIndex) == propertNameChars[charIndex]) {
							match[nameIndex] = true;
							noneMatched = false;
						}
					}
					else {
						char charAfter = propertNameChars[this.names[nameIndex].length()];
						if (charAfter == '.' || charAfter == '_') {
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

}
