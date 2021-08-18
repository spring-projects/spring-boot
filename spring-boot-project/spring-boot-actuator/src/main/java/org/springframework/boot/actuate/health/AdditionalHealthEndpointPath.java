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

package org.springframework.boot.actuate.health;

import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object that represents an additional path for a {@link HealthEndpointGroup}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.6.0
 */
public final class AdditionalHealthEndpointPath {

	private final WebServerNamespace namespace;

	private final String value;

	private final String canonicalValue;

	private AdditionalHealthEndpointPath(WebServerNamespace namespace, String value) {
		this.namespace = namespace;
		this.value = value;
		this.canonicalValue = (!value.startsWith("/")) ? "/" + value : value;
	}

	/**
	 * Returns the {@link WebServerNamespace} associated with this path.
	 * @return the server namespace
	 */
	public WebServerNamespace getNamespace() {
		return this.namespace;
	}

	/**
	 * Returns the value corresponding to this path.
	 * @return the path
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * Returns {@code true} if this path has the given {@link WebServerNamespace}.
	 * @param webServerNamespace the server namespace
	 * @return the new instance
	 */
	public boolean hasNamespace(WebServerNamespace webServerNamespace) {
		return this.namespace.equals(webServerNamespace);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		AdditionalHealthEndpointPath other = (AdditionalHealthEndpointPath) obj;
		boolean result = true;
		result = result && this.namespace.equals(other.namespace);
		result = result && this.canonicalValue.equals(other.canonicalValue);
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.namespace.hashCode();
		result = prime * result + this.canonicalValue.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return this.namespace.getValue() + ":" + this.value;
	}

	/**
	 * Creates an {@link AdditionalHealthEndpointPath} from the given input. The input
	 * must contain a prefix and value separated by a `:`. The value must be limited to
	 * one path segment. For example, `server:/healthz`.
	 * @param value the value to parse
	 * @return the new instance
	 */
	public static AdditionalHealthEndpointPath from(String value) {
		Assert.hasText(value, "Value must not be null");
		String[] values = value.split(":");
		Assert.isTrue(values.length == 2, "Value must contain a valid namespace and value separated by ':'.");
		Assert.isTrue(StringUtils.hasText(values[0]), "Value must contain a valid namespace.");
		WebServerNamespace namespace = WebServerNamespace.from(values[0]);
		validateValue(values[1]);
		return new AdditionalHealthEndpointPath(namespace, values[1]);
	}

	/**
	 * Creates an {@link AdditionalHealthEndpointPath} from the given
	 * {@link WebServerNamespace} and value.
	 * @param webServerNamespace the server namespace
	 * @param value the value
	 * @return the new instance
	 */
	public static AdditionalHealthEndpointPath of(WebServerNamespace webServerNamespace, String value) {
		Assert.notNull(webServerNamespace, "The server namespace must not be null.");
		Assert.notNull(value, "The value must not be null.");
		validateValue(value);
		return new AdditionalHealthEndpointPath(webServerNamespace, value);
	}

	private static void validateValue(String value) {
		Assert.isTrue(StringUtils.countOccurrencesOf(value, "/") <= 1 && value.indexOf("/") <= 0,
				"Value must contain only one segment.");
	}

}
