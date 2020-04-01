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

package org.springframework.boot.buildpack.platform.build;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

/**
 * A set of API Version numbers comprised of major and minor values.
 *
 * @author Scott Frederick
 */
final class ApiVersions {

	/**
	 * The platform API versions supported by this release.
	 */
	static final ApiVersions SUPPORTED_PLATFORMS = new ApiVersions(ApiVersion.of(0, 2), ApiVersion.of(0, 3));

	private final ApiVersion[] apiVersions;

	private ApiVersions(ApiVersion... versions) {
		this.apiVersions = versions;
	}

	/**
	 * Assert that the specified version is supported by these API versions.
	 * @param other the version to check against
	 */
	void assertSupports(ApiVersion other) {
		for (ApiVersion apiVersion : this.apiVersions) {
			if (apiVersion.supports(other)) {
				return;
			}
		}
		throw new IllegalStateException(
				"Detected platform API version '" + other + "' is not included in supported versions '" + this + "'");
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		ApiVersions other = (ApiVersions) obj;
		return Arrays.equals(this.apiVersions, other.apiVersions);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.apiVersions);
	}

	@Override
	public String toString() {
		return StringUtils.arrayToCommaDelimitedString(this.apiVersions);
	}

	/**
	 * Factory method to parse strings into an {@link ApiVersions} instance.
	 * @param values the values to parse.
	 * @return the corresponding {@link ApiVersions}
	 * @throws IllegalArgumentException if any values could not be parsed
	 */
	static ApiVersions parse(String... values) {
		List<ApiVersion> versions = Arrays.stream(values).map(ApiVersion::parse).collect(Collectors.toList());
		return new ApiVersions(versions.toArray(new ApiVersion[] {}));
	}

}
