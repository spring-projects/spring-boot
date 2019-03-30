/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.util.Assert;

/**
 * An identifier for an actuator endpoint. Endpoint IDs may contain only letters, numbers
 * {@code '.'} and {@code '-'}. They must begin with a lower-case letter. Case and syntax
 * characters are ignored when comparing endpoint IDs.
 *
 * @author Phillip Webb
 * @since 2.0.6
 */
public final class EndpointId {

	private static final Log logger = LogFactory.getLog(EndpointId.class);

	private static final Set<String> loggedWarnings = new HashSet<>();

	private static final Pattern VALID_PATTERN = Pattern.compile("[a-zA-Z0-9\\.\\-]+");

	private static final Pattern WARNING_PATTERN = Pattern.compile("[\\.\\-]+");

	private final String value;

	private final String lowerCaseValue;

	private final String lowerCaseAlphaNumeric;

	private EndpointId(String value) {
		Assert.hasText(value, "Value must not be empty");
		Assert.isTrue(VALID_PATTERN.matcher(value).matches(),
				"Value must only contain valid chars");
		Assert.isTrue(!Character.isDigit(value.charAt(0)),
				"Value must not start with a number");
		Assert.isTrue(!Character.isUpperCase(value.charAt(0)),
				"Value must not start with an uppercase letter");
		if (WARNING_PATTERN.matcher(value).find()) {
			logWarning(value);
		}
		this.value = value;
		this.lowerCaseValue = value.toLowerCase(Locale.ENGLISH);
		this.lowerCaseAlphaNumeric = getAlphaNumerics(this.lowerCaseValue);
	}

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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.lowerCaseAlphaNumeric
				.equals(((EndpointId) obj).lowerCaseAlphaNumeric);
	}

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
	 * Factory method to create a new {@link EndpointId} from a property value. More
	 * lenient than {@link #of(String)} to allow for common "relaxed" property variants.
	 * @param value the property value to convert
	 * @return an {@link EndpointId} instance
	 */
	public static EndpointId fromPropertyValue(String value) {
		return new EndpointId(value.replace("-", ""));
	}

	static void resetLoggedWarnings() {
		loggedWarnings.clear();
	}

	private static void logWarning(String value) {
		if (logger.isWarnEnabled() && loggedWarnings.add(value)) {
			logger.warn("Endpoint ID '" + value
					+ "' contains invalid characters, please migrate to a valid format.");
		}
	}

}
