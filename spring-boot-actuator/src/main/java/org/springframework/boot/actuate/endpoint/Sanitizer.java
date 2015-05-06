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

package org.springframework.boot.actuate.endpoint;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Internal strategy used to sanitize potentially sensitive keys.
 *
 * @author Christian Dupuis
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @author Johannes Stelzer
 */
class Sanitizer {

	private static final String[] REGEX_PARTS = { "*", "$", "^", "+" };

	private Pattern[] keysToSanitize;

	private String[] sourcesToSanitize = { "decrypted" };

	public Sanitizer() {
		setKeysToSanitize(new String[] { "password", "secret", "key" });
	}

	/**
	 * Keys that should be sanitized. Keys can be simple strings that the property ends
	 * with or regex expressions.
	 * @param keysToSanitize the keys to sanitize
	 */
	public void setKeysToSanitize(String... keysToSanitize) {
		Assert.notNull(keysToSanitize, "KeysToSanitize must not be null");
		this.keysToSanitize = new Pattern[keysToSanitize.length];
		for (int i = 0; i < keysToSanitize.length; i++) {
			this.keysToSanitize[i] = getPattern(keysToSanitize[i]);
		}
	}

	/**
	 * PropertySources whose properties should be sanitized.
	 * @param sourcesToSanitize name of the propertySource to sanitize
	 */
	public void setSourcesToSanitized(String... sourcesToSanitize) {
		Assert.notNull(sourcesToSanitize, "sourcesToSanitize must not be null");
		this.sourcesToSanitize = Arrays.copyOf(sourcesToSanitize,
				sourcesToSanitize.length);
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
	 * @param sourceName propertySourcename to sanitize
	 * @param key the key to sanitize
	 * @param value the value
	 * @return the potentially sanitized value
	 */
	public Object sanitize(String sourceName, String key, Object value) {
		if (value == null) {
			return null;
		}
		for (String source : this.sourcesToSanitize) {
			if (source.equalsIgnoreCase(sourceName)) {
				return "******";
			}
		}
		for (Pattern pattern : this.keysToSanitize) {
			if (pattern.matcher(key).matches()) {
				return "******";
			}
		}
		return value;
	}

}
