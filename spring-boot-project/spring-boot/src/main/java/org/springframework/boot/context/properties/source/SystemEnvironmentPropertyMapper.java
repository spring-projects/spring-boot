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

package org.springframework.boot.context.properties.source;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;

/**
 * {@link PropertyMapper} for system environment variables. Names are mapped by removing
 * invalid characters, converting to lower case and replacing "{@code _}" with
 * "{@code .}". For example, "{@code SERVER_PORT}" is mapped to "{@code server.port}". In
 * addition, numeric elements are mapped to indexes (e.g. "{@code HOST_0}" is mapped to
 * "{@code host[0]}").
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see SpringConfigurationPropertySource
 */
final class SystemEnvironmentPropertyMapper implements PropertyMapper {

	public static final PropertyMapper INSTANCE = new SystemEnvironmentPropertyMapper();

	/**
     * Maps the given ConfigurationPropertyName to a list of property names.
     * 
     * @param configurationPropertyName the ConfigurationPropertyName to be mapped
     * @return a list of property names
     */
    @Override
	public List<String> map(ConfigurationPropertyName configurationPropertyName) {
		String name = convertName(configurationPropertyName);
		String legacyName = convertLegacyName(configurationPropertyName);
		if (name.equals(legacyName)) {
			return Collections.singletonList(name);
		}
		return Arrays.asList(name, legacyName);
	}

	/**
     * Converts the given ConfigurationPropertyName to a String representation.
     * 
     * @param name the ConfigurationPropertyName to be converted
     * @return the String representation of the ConfigurationPropertyName
     */
    private String convertName(ConfigurationPropertyName name) {
		return convertName(name, name.getNumberOfElements());
	}

	/**
     * Converts the given ConfigurationPropertyName to a string representation with the specified number of elements.
     * 
     * @param name The ConfigurationPropertyName to convert
     * @param numberOfElements The number of elements to include in the string representation
     * @return The converted string representation of the ConfigurationPropertyName
     */
    private String convertName(ConfigurationPropertyName name, int numberOfElements) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < numberOfElements; i++) {
			if (!result.isEmpty()) {
				result.append('_');
			}
			result.append(name.getElement(i, Form.UNIFORM).toUpperCase(Locale.ENGLISH));
		}
		return result.toString();
	}

	/**
     * Converts a legacy name to a new name using the provided ConfigurationPropertyName object.
     * 
     * @param name the ConfigurationPropertyName object representing the legacy name
     * @return the converted name as a String
     */
    private String convertLegacyName(ConfigurationPropertyName name) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < name.getNumberOfElements(); i++) {
			if (!result.isEmpty()) {
				result.append('_');
			}
			result.append(convertLegacyNameElement(name.getElement(i, Form.ORIGINAL)));
		}
		return result.toString();
	}

	/**
     * Converts a legacy name element to a standardized format.
     * Replaces hyphens with underscores and converts the element to uppercase.
     * 
     * @param element the legacy name element to be converted
     * @return the converted name element
     */
    private Object convertLegacyNameElement(String element) {
		return element.replace('-', '_').toUpperCase(Locale.ENGLISH);
	}

	/**
     * Maps the given property source name to a ConfigurationPropertyName object.
     * 
     * @param propertySourceName the name of the property source
     * @return the ConfigurationPropertyName object representing the mapped name
     */
    @Override
	public ConfigurationPropertyName map(String propertySourceName) {
		return convertName(propertySourceName);
	}

	/**
     * Converts the given property source name to a ConfigurationPropertyName object.
     * 
     * @param propertySourceName the property source name to be converted
     * @return the converted ConfigurationPropertyName object
     * @throws IllegalArgumentException if the property source name is invalid
     */
    private ConfigurationPropertyName convertName(String propertySourceName) {
		try {
			return ConfigurationPropertyName.adapt(propertySourceName, '_', this::processElementValue);
		}
		catch (Exception ex) {
			return ConfigurationPropertyName.EMPTY;
		}
	}

	/**
     * Processes the value of an element.
     * 
     * @param value the value to be processed
     * @return the processed value
     */
    private CharSequence processElementValue(CharSequence value) {
		String result = value.toString().toLowerCase(Locale.ENGLISH);
		return isNumber(result) ? "[" + result + "]" : result;
	}

	/**
     * Checks if a given string is a number.
     * 
     * @param string the string to be checked
     * @return true if the string is a number, false otherwise
     */
    private static boolean isNumber(String string) {
		return string.chars().allMatch(Character::isDigit);
	}

	/**
     * Returns a BiPredicate that checks if the first ConfigurationPropertyName is an ancestor of the second ConfigurationPropertyName.
     *
     * @return the BiPredicate that checks if the first ConfigurationPropertyName is an ancestor of the second ConfigurationPropertyName
     */
    @Override
	public BiPredicate<ConfigurationPropertyName, ConfigurationPropertyName> getAncestorOfCheck() {
		return this::isAncestorOf;
	}

	/**
     * Checks if the given {@link ConfigurationPropertyName} is an ancestor of the candidate {@link ConfigurationPropertyName}.
     * An ancestor is a property name that is a parent or grandparent of the candidate.
     * 
     * @param name the {@link ConfigurationPropertyName} to check if it is an ancestor
     * @param candidate the candidate {@link ConfigurationPropertyName} to check against
     * @return {@code true} if the given name is an ancestor of the candidate, {@code false} otherwise
     */
    private boolean isAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
		return name.isAncestorOf(candidate) || isLegacyAncestorOf(name, candidate);
	}

	/**
     * Checks if the given {@link ConfigurationPropertyName} is a legacy ancestor of the candidate {@link ConfigurationPropertyName}.
     * 
     * @param name the {@link ConfigurationPropertyName} to check if it is a legacy ancestor
     * @param candidate the candidate {@link ConfigurationPropertyName} to check against
     * @return {@code true} if the given {@link ConfigurationPropertyName} is a legacy ancestor of the candidate, {@code false} otherwise
     */
    private boolean isLegacyAncestorOf(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
		if (!hasDashedEntries(name)) {
			return false;
		}
		ConfigurationPropertyName legacyCompatibleName = buildLegacyCompatibleName(name);
		return legacyCompatibleName != null && legacyCompatibleName.isAncestorOf(candidate);
	}

	/**
     * Builds a legacy compatible configuration property name by replacing dashes with dots.
     * 
     * @param name the original configuration property name
     * @return the legacy compatible configuration property name
     */
    private ConfigurationPropertyName buildLegacyCompatibleName(ConfigurationPropertyName name) {
		StringBuilder legacyCompatibleName = new StringBuilder();
		for (int i = 0; i < name.getNumberOfElements(); i++) {
			if (i != 0) {
				legacyCompatibleName.append('.');
			}
			legacyCompatibleName.append(name.getElement(i, Form.DASHED).replace('-', '.'));
		}
		return ConfigurationPropertyName.ofIfValid(legacyCompatibleName);
	}

	/**
     * Checks if the given ConfigurationPropertyName has any dashed entries.
     * 
     * @param name the ConfigurationPropertyName to check
     * @return true if the ConfigurationPropertyName has dashed entries, false otherwise
     */
    boolean hasDashedEntries(ConfigurationPropertyName name) {
		for (int i = 0; i < name.getNumberOfElements(); i++) {
			if (name.getElement(i, Form.DASHED).indexOf('-') != -1) {
				return true;
			}
		}
		return false;
	}

}
