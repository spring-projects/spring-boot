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

package org.springframework.boot.loader.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Helper class for resolving placeholders in texts. Usually applied to file paths.
 * <p>
 * A text may contain {@code $ ...} placeholders, to be resolved as system properties:
 * e.g. {@code $ user.dir}. Default values can be supplied using the ":" separator between
 * key and value.
 * <p>
 * Adapted from Spring.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @since 1.0.0
 * @see System#getProperty(String)
 */
public abstract class SystemPropertyUtils {

	/**
	 * Prefix for system property placeholders: "${".
	 */
	public static final String PLACEHOLDER_PREFIX = "${";

	/**
	 * Suffix for system property placeholders: "}".
	 */
	public static final String PLACEHOLDER_SUFFIX = "}";

	/**
	 * Value separator for system property placeholders: ":".
	 */
	public static final String VALUE_SEPARATOR = ":";

	private static final String SIMPLE_PREFIX = PLACEHOLDER_PREFIX.substring(1);

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * system property values.
	 * @param text the String to resolve
	 * @return the resolved String
	 * @throws IllegalArgumentException if there is an unresolvable placeholder
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 */
	public static String resolvePlaceholders(String text) {
		if (text == null) {
			return text;
		}
		return parseStringValue(null, text, text, new HashSet<>());
	}

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * system property values.
	 * @param properties a properties instance to use in addition to System
	 * @param text the String to resolve
	 * @return the resolved String
	 * @throws IllegalArgumentException if there is an unresolvable placeholder
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 */
	public static String resolvePlaceholders(Properties properties, String text) {
		if (text == null) {
			return text;
		}
		return parseStringValue(properties, text, text, new HashSet<>());
	}

	/**
	 * Parses a string value containing placeholders and resolves them using the provided
	 * properties.
	 * @param properties the properties to use for resolving placeholders
	 * @param value the string value containing placeholders
	 * @param current the current placeholder being parsed
	 * @param visitedPlaceholders a set of visited placeholders to detect circular
	 * references
	 * @return the string value with all placeholders resolved
	 * @throws IllegalArgumentException if a circular placeholder reference is detected
	 */
	private static String parseStringValue(Properties properties, String value, String current,
			Set<String> visitedPlaceholders) {

		StringBuilder buf = new StringBuilder(current);

		int startIndex = current.indexOf(PLACEHOLDER_PREFIX);
		while (startIndex != -1) {
			int endIndex = findPlaceholderEndIndex(buf, startIndex);
			if (endIndex != -1) {
				String placeholder = buf.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
				String originalPlaceholder = placeholder;
				if (!visitedPlaceholders.add(originalPlaceholder)) {
					throw new IllegalArgumentException(
							"Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
				}
				// Recursive invocation, parsing placeholders contained in the
				// placeholder
				// key.
				placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
				// Now obtain the value for the fully resolved key...
				String propVal = resolvePlaceholder(properties, value, placeholder);
				if (propVal == null) {
					int separatorIndex = placeholder.indexOf(VALUE_SEPARATOR);
					if (separatorIndex != -1) {
						String actualPlaceholder = placeholder.substring(0, separatorIndex);
						String defaultValue = placeholder.substring(separatorIndex + VALUE_SEPARATOR.length());
						propVal = resolvePlaceholder(properties, value, actualPlaceholder);
						if (propVal == null) {
							propVal = defaultValue;
						}
					}
				}
				if (propVal != null) {
					// Recursive invocation, parsing placeholders contained in the
					// previously resolved placeholder value.
					propVal = parseStringValue(properties, value, propVal, visitedPlaceholders);
					buf.replace(startIndex, endIndex + PLACEHOLDER_SUFFIX.length(), propVal);
					startIndex = buf.indexOf(PLACEHOLDER_PREFIX, startIndex + propVal.length());
				}
				else {
					// Proceed with unprocessed value.
					startIndex = buf.indexOf(PLACEHOLDER_PREFIX, endIndex + PLACEHOLDER_SUFFIX.length());
				}
				visitedPlaceholders.remove(originalPlaceholder);
			}
			else {
				startIndex = -1;
			}
		}

		return buf.toString();
	}

	/**
	 * Resolves a placeholder in the given text using the provided properties and
	 * placeholder name.
	 * @param properties the properties to use for resolving the placeholder
	 * @param text the text containing the placeholder
	 * @param placeholderName the name of the placeholder to resolve
	 * @return the resolved value of the placeholder, or null if it could not be resolved
	 */
	private static String resolvePlaceholder(Properties properties, String text, String placeholderName) {
		String propVal = getProperty(placeholderName, null, text);
		if (propVal != null) {
			return propVal;
		}
		return (properties != null) ? properties.getProperty(placeholderName) : null;
	}

	/**
	 * Retrieves the value of the specified property key from the system properties. If
	 * the property key is not found, the default value provided will be returned.
	 * @param key the key of the property to retrieve
	 * @return the value of the property, or the default value if not found
	 */
	public static String getProperty(String key) {
		return getProperty(key, null, "");
	}

	/**
	 * Retrieves the value of the specified property key from the system properties. If
	 * the property key is not found, the default value is returned.
	 * @param key the property key to retrieve the value for
	 * @param defaultValue the default value to return if the property key is not found
	 * @return the value of the property key if found, otherwise the default value
	 */
	public static String getProperty(String key, String defaultValue) {
		return getProperty(key, defaultValue, "");
	}

	/**
	 * Search the System properties and environment variables for a value with the
	 * provided key. Environment variables in {@code UPPER_CASE} style are allowed where
	 * System properties would normally be {@code lower.case}.
	 * @param key the key to resolve
	 * @param defaultValue the default value
	 * @param text optional extra context for an error message if the key resolution fails
	 * (e.g. if System properties are not accessible)
	 * @return a static property value or null of not found
	 */
	public static String getProperty(String key, String defaultValue, String text) {
		try {
			String propVal = System.getProperty(key);
			if (propVal == null) {
				// Fall back to searching the system environment.
				propVal = System.getenv(key);
			}
			if (propVal == null) {
				// Try with underscores.
				String name = key.replace('.', '_');
				propVal = System.getenv(name);
			}
			if (propVal == null) {
				// Try uppercase with underscores as well.
				String name = key.toUpperCase(Locale.ENGLISH).replace('.', '_');
				propVal = System.getenv(name);
			}
			if (propVal != null) {
				return propVal;
			}
		}
		catch (Throwable ex) {
			System.err.println("Could not resolve key '" + key + "' in '" + text
					+ "' as system property or in environment: " + ex);
		}
		return defaultValue;
	}

	/**
	 * Finds the end index of a placeholder in the given character sequence starting from
	 * the specified index.
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
