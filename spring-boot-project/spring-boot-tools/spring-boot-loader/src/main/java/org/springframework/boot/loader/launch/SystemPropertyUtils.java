/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.launch;

import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Internal helper class adapted from Spring Framework for resolving placeholders in
 * texts.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Phillip Webb
 */
final class SystemPropertyUtils {

	private static final String PLACEHOLDER_PREFIX = "${";

	private static final String PLACEHOLDER_SUFFIX = "}";

	private static final String VALUE_SEPARATOR = ":";

	private static final String SIMPLE_PREFIX = PLACEHOLDER_PREFIX.substring(1);

	private SystemPropertyUtils() {
	}

	static String resolvePlaceholders(Properties properties, String text) {
		return (text != null) ? parseStringValue(properties, text, text, new HashSet<>()) : null;
	}

	private static String parseStringValue(Properties properties, String value, String current,
			Set<String> visitedPlaceholders) {
		StringBuilder result = new StringBuilder(current);
		int startIndex = current.indexOf(PLACEHOLDER_PREFIX);
		while (startIndex != -1) {
			int endIndex = findPlaceholderEndIndex(result, startIndex);
			if (endIndex == -1) {
				startIndex = -1;
				continue;
			}
			String placeholder = result.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
			String originalPlaceholder = placeholder;
			if (!visitedPlaceholders.add(originalPlaceholder)) {
				throw new IllegalArgumentException(
						"Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
			}
			placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
			String propertyValue = resolvePlaceholder(properties, value, placeholder);
			if (propertyValue == null) {
				int separatorIndex = placeholder.indexOf(VALUE_SEPARATOR);
				if (separatorIndex != -1) {
					String actualPlaceholder = placeholder.substring(0, separatorIndex);
					String defaultValue = placeholder.substring(separatorIndex + VALUE_SEPARATOR.length());
					propertyValue = resolvePlaceholder(properties, value, actualPlaceholder);
					propertyValue = (propertyValue != null) ? propertyValue : defaultValue;
				}
			}
			if (propertyValue != null) {
				propertyValue = parseStringValue(properties, value, propertyValue, visitedPlaceholders);
				result.replace(startIndex, endIndex + PLACEHOLDER_SUFFIX.length(), propertyValue);
				startIndex = result.indexOf(PLACEHOLDER_PREFIX, startIndex + propertyValue.length());
			}
			else {
				startIndex = result.indexOf(PLACEHOLDER_PREFIX, endIndex + PLACEHOLDER_SUFFIX.length());
			}
			visitedPlaceholders.remove(originalPlaceholder);
		}
		return result.toString();
	}

	private static String resolvePlaceholder(Properties properties, String text, String placeholderName) {
		String propertyValue = getProperty(placeholderName, null, text);
		if (propertyValue != null) {
			return propertyValue;
		}
		return (properties != null) ? properties.getProperty(placeholderName) : null;
	}

	static String getProperty(String key) {
		return getProperty(key, null, "");
	}

	private static String getProperty(String key, String defaultValue, String text) {
		try {
			String value = System.getProperty(key);
			value = (value != null) ? value : System.getenv(key);
			value = (value != null) ? value : System.getenv(key.replace('.', '_'));
			value = (value != null) ? value : System.getenv(key.toUpperCase(Locale.ENGLISH).replace('.', '_'));
			return (value != null) ? value : defaultValue;
		}
		catch (Throwable ex) {
			System.err.println("Could not resolve key '" + key + "' in '" + text
					+ "' as system property or in environment: " + ex);
			return defaultValue;
		}
	}

	private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
		int index = startIndex + PLACEHOLDER_PREFIX.length();
		int withinNestedPlaceholder = 0;
		while (index < buf.length()) {
			if (substringMatch(buf, index, PLACEHOLDER_SUFFIX)) {
				if (withinNestedPlaceholder > 0) {
					withinNestedPlaceholder--;
					index = index + PLACEHOLDER_SUFFIX.length();
				}
				else {
					return index;
				}
			}
			else if (substringMatch(buf, index, SIMPLE_PREFIX)) {
				withinNestedPlaceholder++;
				index = index + SIMPLE_PREFIX.length();
			}
			else {
				index++;
			}
		}
		return -1;
	}

	private static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
		for (int j = 0; j < substring.length(); j++) {
			int i = index + j;
			if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
				return false;
			}
		}
		return true;
	}

}
