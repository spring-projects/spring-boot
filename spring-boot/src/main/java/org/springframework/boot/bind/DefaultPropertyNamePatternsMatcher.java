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

import org.springframework.util.StringUtils;

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

	private final RelaxedNames[] relaxed;

	public DefaultPropertyNamePatternsMatcher(String... names) {
		this(new HashSet<String>(Arrays.asList(names)));
	}

	public DefaultPropertyNamePatternsMatcher(Set<String> names) {
		this.names = names.toArray(new String[names.size()]);
		this.relaxed = new RelaxedNames[this.names.length];
	}

	@Override
	public boolean matches(String propertyName) {
		if (propertyName.contains("[")) {
			propertyName = propertyName.substring(0, propertyName.indexOf("["));
		}
		for (int i = 0; i < this.names.length; i++) {
			if (this.relaxed[i] == null) {
				this.relaxed[i] = new RelaxedNames(this.names[i]);
			}
			for (String relaxedName : this.relaxed[i]) {
				if (relaxedName.equals(propertyName)) {
					return true;
				}
			}
			if (matchWithSeparator(this.names[i], propertyName, "._")) {
				return true;
			}
		}
		return false;
	}

	private boolean matchWithSeparator(String targetName, String propertyName,
			String separator) {
		String[] targetNameTokens = StringUtils.tokenizeToStringArray(targetName,
				separator);
		String[] propertyNameTokens = StringUtils.tokenizeToStringArray(propertyName,
				separator);
		for (int j = 0; j < propertyNameTokens.length; j++) {
			if (j >= targetNameTokens.length) {
				// No match and nothing left to match on
				break;
			}
			if (targetNameTokens[j].equalsIgnoreCase(propertyNameTokens[j])) {
				return true;
			}
		}
		return false;
	}

}
