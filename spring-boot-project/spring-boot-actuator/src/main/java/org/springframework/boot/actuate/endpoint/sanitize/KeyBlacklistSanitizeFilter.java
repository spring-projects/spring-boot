/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.sanitize;

import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Sanitize filter operating on String keys. Matches if the given key contains any of the
 * configured words. Supports basic regex syntax.
 *
 * @author Piotr Betkier
 * @since 2.0.0
 */
public final class KeyBlacklistSanitizeFilter implements Sanitizer.Filter<String> {

	private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

	private final Pattern[] keysToSanitize;

	private KeyBlacklistSanitizeFilter(Pattern[] patterns) {
		this.keysToSanitize = patterns;
	}

	/**
	 * Creates the filter based on the default blacklist.
	 * @return a new filter instance
	 */
	public static KeyBlacklistSanitizeFilter matchingDefaultKeys() {
		return KeyBlacklistSanitizeFilter.matchingKeys("password", "secret", "key",
				"token", ".*credentials.*", "vcap_services");
	}

	/**
	 * Creates the filter based on the given blacklist.
	 * @param keysToSanitize keys to match
	 * @return a new filter instance
	 */
	public static KeyBlacklistSanitizeFilter matchingKeys(String... keysToSanitize) {
		Assert.notNull(keysToSanitize, "KeysToSanitize must not be null");
		Pattern[] patterns = new Pattern[keysToSanitize.length];
		for (int i = 0; i < keysToSanitize.length; i++) {
			patterns[i] = getPattern(keysToSanitize[i]);
		}
		return new KeyBlacklistSanitizeFilter(patterns);
	}

	private static Pattern getPattern(String value) {
		if (isRegex(value)) {
			return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
		}
		return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
	}

	private static boolean isRegex(String value) {
		for (String part : REGEX_PARTS) {
			if (value.contains(part)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean match(String key) {
		for (Pattern pattern : this.keysToSanitize) {
			if (pattern.matcher(key).matches()) {
				return true;
			}
		}
		return false;
	}
}
