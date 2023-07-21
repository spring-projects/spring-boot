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

	private WebServerNamespace(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static WebServerNamespace from(String value) {
		if (StringUtils.hasText(value)) {
			return new WebServerNamespace(value);
		}
		return SERVER;
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

}
