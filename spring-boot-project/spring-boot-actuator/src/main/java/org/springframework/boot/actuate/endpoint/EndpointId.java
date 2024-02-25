/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * An identifier for an actuator endpoint. Endpoint IDs may contain only letters and
 * numbers. They must begin with a lower-case letter. Case and syntax characters are
 * ignored when comparing endpoint IDs.
 *
 * @author Phillip Webb
 * @since 2.0.6
 */
public final class EndpointId {

	private static final Log logger = LogFactory.getLog(EndpointId.class);

	private static final Set<String> loggedWarnings = new HashSet<>();

	private static final Pattern VALID_PATTERN = Pattern.compile("[a-zA-Z0-9.-]+");

	private static final Pattern WARNING_PATTERN = Pattern.compile("[.-]+");

	private static final String MIGRATE_LEGACY_NAMES_PROPERTY = "management.endpoints.migrate-legacy-ids";

	private final String value;

	private final String lowerCaseValue;

	private final String lowerCaseAlphaNumeric;

	/**
     * Constructs a new EndpointId with the given value.
     * 
     * @param value the value of the EndpointId
     * @throws IllegalArgumentException if the value is empty, contains invalid characters, starts with a number, or starts with an uppercase letter
     */
    private EndpointId(String value) {
		Assert.hasText(value, "Value must not be empty");
		Assert.isTrue(VALID_PATTERN.matcher(value).matches(), "Value must only contain valid chars");
		Assert.isTrue(!Character.isDigit(value.charAt(0)), "Value must not start with a number");
		Assert.isTrue(!Character.isUpperCase(value.charAt(0)), "Value must not start with an uppercase letter");
		if (WARNING_PATTERN.matcher(value).find()) {
			logWarning(value);
		}
		this.value = value;
		this.lowerCaseValue = value.toLowerCase(Locale.ENGLISH);
		this.lowerCaseAlphaNumeric = getAlphaNumerics(this.lowerCaseValue);
	}

	/**
     * Returns a string containing only alphanumeric characters from the given value.
     * 
     * @param value the input string
     * @return a string containing only alphanumeric characters
     */
    private String getAlphaNumerics(String value) {
		StringBuilder result = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9') {
				result.append(ch);
			}
		}
		return result.toString();
	}

	/**
     * Compares this EndpointId object to the specified object. The result is true if and only if the argument is not null and is an EndpointId object that represents the same lower case alphanumeric value as this object.
     *
     * @param obj the object to compare this EndpointId against
     * @return true if the given object represents an EndpointId equivalent to this EndpointId, false otherwise
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.lowerCaseAlphaNumeric.equals(((EndpointId) obj).lowerCaseAlphaNumeric);
	}

	/**
     * Returns the hash code value for the EndpointId object.
     * 
     * @return the hash code value for the EndpointId object
     */
    @Override
	public int hashCode() {
		return this.lowerCaseAlphaNumeric.hashCode();
	}

	/**
	 * Return a lower-case version of the endpoint ID.
	 * @return the lower-case endpoint ID
	 */
	public String toLowerCaseString() {
		return this.lowerCaseValue;
	}

	/**
     * Returns a string representation of the EndpointId object.
     *
     * @return the string representation of the EndpointId object
     */
    @Override
	public String toString() {
		return this.value;
	}

	/**
	 * Factory method to create a new {@link EndpointId} of the specified value.
	 * @param value the endpoint ID value
	 * @return an {@link EndpointId} instance
	 */
	public static EndpointId of(String value) {
		return new EndpointId(value);
	}

	/**
	 * Factory method to create a new {@link EndpointId} of the specified value. This
	 * variant will respect the {@code management.endpoints.migrate-legacy-names} property
	 * if it has been set in the {@link Environment}.
	 * @param environment the Spring environment
	 * @param value the endpoint ID value
	 * @return an {@link EndpointId} instance
	 * @since 2.2.0
	 */
	public static EndpointId of(Environment environment, String value) {
		Assert.notNull(environment, "Environment must not be null");
		return new EndpointId(migrateLegacyId(environment, value));
	}

	/**
     * Migrates the legacy ID by removing any occurrences of hyphens or dots in the given value.
     * 
     * @param environment the environment object containing the property for legacy ID migration
     * @param value the legacy ID value to be migrated
     * @return the migrated legacy ID value
     */
    private static String migrateLegacyId(Environment environment, String value) {
		if (environment.getProperty(MIGRATE_LEGACY_NAMES_PROPERTY, Boolean.class, false)) {
			return value.replaceAll("[-.]+", "");
		}
		return value;
	}

	/**
	 * Factory method to create a new {@link EndpointId} from a property value. More
	 * lenient than {@link #of(String)} to allow for common "relaxed" property variants.
	 * @param value the property value to convert
	 * @return an {@link EndpointId} instance
	 */
	public static EndpointId fromPropertyValue(String value) {
		return new EndpointId(value.replace("-", ""));
	}

	/**
     * Resets the list of logged warnings.
     */
    static void resetLoggedWarnings() {
		loggedWarnings.clear();
	}

	/**
     * Logs a warning message if the provided value contains invalid characters.
     * 
     * @param value the endpoint ID value to check
     */
    private static void logWarning(String value) {
		if (logger.isWarnEnabled() && loggedWarnings.add(value)) {
			logger.warn("Endpoint ID '" + value + "' contains invalid characters, please migrate to a valid format.");
		}
	}

}
