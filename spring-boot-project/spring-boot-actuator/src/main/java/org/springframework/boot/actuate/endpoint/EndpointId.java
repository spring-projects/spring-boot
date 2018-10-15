/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * An identifier for an actuator endpoint. Endpoint IDs may contain only letters, numbers
 * and {@code '.'}. They must begin with a lower-case letter. Case is ignored when
 * comparing endpoint IDs.
 *
 * @author Phillip Webb
 * @since 2.0.6
 */
public final class EndpointId {

	private static final Pattern VALID_CHARS = Pattern.compile("[a-zA-Z0-9\\.]+");

	private final String value;

	private final String lowerCaseValue;

	private EndpointId(String value) {
		Assert.hasText(value, "Value must not be empty");
		Assert.isTrue(VALID_CHARS.matcher(value).matches(),
				"Value must be alpha-numeric or '.'");
		Assert.isTrue(!Character.isDigit(value.charAt(0)),
				"Value must not start with a number");
		Assert.isTrue(!Character.isUpperCase(value.charAt(0)),
				"Value must not start with an uppercase letter");
		this.value = value;
		this.lowerCaseValue = value.toLowerCase(Locale.ENGLISH);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return toLowerCaseString().equals(((EndpointId) obj).toLowerCaseString());
	}

	@Override
	public int hashCode() {
		return toLowerCaseString().hashCode();
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
	 * Factory method to create a new {@link EndpointId} from a property value. Is more
	 * lenient that {@link #of(String)} to allow for common "relaxed" property variants.
	 * @param value the property value to convert
	 * @return an {@link EndpointId} instance
	 */
	public static EndpointId fromPropertyValue(String value) {
		return new EndpointId(value.replace("-", ""));
	}

}
