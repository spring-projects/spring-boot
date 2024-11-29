/*
 * Copyright 2012-2024 the original author or authors.
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
 * A web server namespace used for disambiguation when multiple web servers are running in
 * the same application (for example a management context running on a different port).
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

	private WebServerNamespace(String value) {
		this.value = value;
	}

	/**
	 * Return the value of the namespace.
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

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

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Factory method to create a new {@link WebServerNamespace} from a value. If the
	 * value is empty or {@code null} then {@link #SERVER} is returned.
	 * @param value the namespace value or {@code null}
	 * @return the web server namespace
	 */
	public static WebServerNamespace from(String value) {
		if (StringUtils.hasText(value)) {
			return new WebServerNamespace(value);
		}
		return SERVER;
	}

}
