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

	/**
     * Private constructor for the SystemPropertyUtils class.
     */
    private SystemPropertyUtils() {
	}

	/**
     * Resolves placeholders in the given text using the provided properties.
     * 
     * @param properties the properties to use for resolving placeholders
     * @param text the text containing placeholders to be resolved
     * @return the text with resolved placeholders, or null if the input text is null
     */
    static String resolvePlaceholders(Properties properties, String text) {
		return (text != null) ? parseStringValue(properties, text, text, new HashSet<>()) : null;
	}

	/**
     * Parses the given value string, replacing any placeholders with their corresponding values from the provided properties.
     * 
     * @param properties the properties containing the placeholder values
     * @param value the value string to be parsed
     * @param current the current value being parsed
     * @param visitedPlaceholders a set of visited placeholders to detect circular references
     * @return the parsed value string with placeholders replaced
     * @throws IllegalArgumentException if a circular placeholder reference is detected
     */
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

	/**
     * Resolves a placeholder in the given text using the provided properties and placeholder name.
     * 
     * @param properties the properties to use for resolving the placeholder
     * @param text the text containing the placeholder
     * @param placeholderName the name of the placeholder to resolve
     * @return the resolved value of the placeholder, or null if it cannot be resolved
     */
    private static String resolvePlaceholder(Properties properties, String text, String placeholderName) {
		String propertyValue = getProperty(placeholderName, null, text);
		if (propertyValue != null) {
			return propertyValue;
		}
		return (properties != null) ? properties.getProperty(placeholderName) : null;
	}

	/**
     * Retrieves the value of the specified property.
     * 
     * @param key the key of the property to retrieve
     * @return the value of the property, or an empty string if the property is not found
     */
    static String getProperty(String key) {
		return getProperty(key, null, "");
	}

	/**
     * Retrieves the value of a system property or environment variable based on the provided key.
     * If the value is not found, the default value is returned.
     * 
     * @param key the key to search for the value
     * @param defaultValue the default value to return if the value is not found
     * @param text the text containing the key, used for error logging
     * @return the value of the system property or environment variable, or the default value if not found
     * @throws Throwable if an error occurs while retrieving the value
     */
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

	/**
     * Finds the end index of a placeholder in the given character sequence starting from the specified index.
     * 
     * @param buf the character sequence to search in
     * @param startIndex the starting index to search from
     * @return the end index of the placeholder, or -1 if not found
     */
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

	/**
     * Checks if a substring matches a portion of a given string.
     * 
     * @param str the string to check
     * @param index the starting index in the string to check from
     * @param substring the substring to match
     * @return true if the substring matches the portion of the string, false otherwise
     */
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
