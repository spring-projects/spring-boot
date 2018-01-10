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

package org.springframework.boot.actuate.endpoint;

import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Strategy that should be used by endpoint implementations to sanitize potentially
 * sensitive keys.
 *
 * @author Christian Dupuis
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @author Nicolas Lejeune
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class Sanitizer {

	private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

	private Pattern[] keysToSanitize;

	public Sanitizer() {
		this("password", "secret", "key", "token", ".*credentials.*", "vcap_services");
	}

	public Sanitizer(String... keysToSanitize) {
		setKeysToSanitize(keysToSanitize);
	}

	/**
	 * Keys that should be sanitized. Keys can be simple strings that the property ends
	 * with or regular expressions.
	 * @param keysToSanitize the keys to sanitize
	 */
	public void setKeysToSanitize(String... keysToSanitize) {
		Assert.notNull(keysToSanitize, "KeysToSanitize must not be null");
		this.keysToSanitize = new Pattern[keysToSanitize.length];
		for (int i = 0; i < keysToSanitize.length; i++) {
			this.keysToSanitize[i] = getPattern(keysToSanitize[i]);
		}
	}

	private Pattern getPattern(String value) {
		if (isRegex(value)) {
			return Pattern.compile(value, Pattern.CASE_INSENSITIVE);
		}
		return Pattern.compile(".*" + value + "$", Pattern.CASE_INSENSITIVE);
	}

	private boolean isRegex(String value) {
		for (String part : REGEX_PARTS) {
			if (value.contains(part)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Sanitize the given value if necessary.
	 * @param key the key to sanitize
	 * @param value the value
	 * @return the potentially sanitized value
	 */
	public Object sanitize(String key, Object value) {
		if (value == null) {
			return null;
		}
		for (Pattern pattern : this.keysToSanitize) {
			if (pattern.matcher(key).matches()) {
				return "******";
			}
		}
		return value;
	}

}
