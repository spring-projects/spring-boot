/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.util.StringUtils;

/**
 * Enumeration of server namespaces.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.6.0
 */
public final class WebServerNamespace {

	/**
	 * {@link WebServerNamespace} that represents the main server.
	 */
	public static final WebServerNamespace SERVER = new WebServerNamespace("server");

	/**
	 * {@link WebServerNamespace} that represents the management server.
	 */
	public static final WebServerNamespace MANAGEMENT = new WebServerNamespace("management");

	private final String value;

	/**
	 * Constructs a new WebServerNamespace with the specified value.
	 * @param value the value of the WebServerNamespace
	 */
	private WebServerNamespace(String value) {
		this.value = value;
	}

	/**
	 * Returns the value of the WebServerNamespace.
	 * @return the value of the WebServerNamespace
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Creates a WebServerNamespace object from the given value.
	 * @param value the value to create the WebServerNamespace object from
	 * @return a WebServerNamespace object created from the given value
	 */
	public static WebServerNamespace from(String value) {
		if (StringUtils.hasText(value)) {
			return new WebServerNamespace(value);
		}
		return SERVER;
	}

	/**
	 * Compares this WebServerNamespace object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the specified object is equal to this WebServerNamespace object,
	 * false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		WebServerNamespace other = (WebServerNamespace) obj;
		return this.value.equals(other.value);
	}

	/**
	 * Returns the hash code value for this WebServerNamespace object.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

}
