/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.yaml;

import java.util.Properties;
import java.util.Set;

import org.springframework.boot.yaml.YamlProcessor.DocumentMatcher;
import org.springframework.boot.yaml.YamlProcessor.MatchStatus;
import org.springframework.util.StringUtils;

/**
 * Matches a document containing a given key and where the value of that key is an array
 * containing one of the given values, or where one of the values matches one of the given
 * values (interpreted as regexes).
 *
 * @author Dave Syer
 */
public class ArrayDocumentMatcher implements DocumentMatcher {

	private final String key;

	private final String[] patterns;

	public ArrayDocumentMatcher(final String key, final String... patterns) {
		this.key = key;
		this.patterns = patterns;

	}

	@Override
	public MatchStatus matches(Properties properties) {
		if (!properties.containsKey(this.key)) {
			return MatchStatus.ABSTAIN;
		}
		Set<String> values = StringUtils.commaDelimitedListToSet(properties
				.getProperty(this.key));
		for (String pattern : this.patterns) {
			for (String value : values) {
				if (value.matches(pattern)) {
					return MatchStatus.FOUND;
				}
			}
		}
		return MatchStatus.NOT_FOUND;
	}

}
