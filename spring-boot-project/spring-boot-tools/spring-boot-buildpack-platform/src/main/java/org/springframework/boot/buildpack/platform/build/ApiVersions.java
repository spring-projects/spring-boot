/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.stream.IntStream;

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
	static final ApiVersions SUPPORTED_PLATFORMS = ApiVersions.of(0, IntStream.rangeClosed(3, 9));

	private final ApiVersion[] apiVersions;

	private ApiVersions(ApiVersion... versions) {
		this.apiVersions = versions;
	}

	/**
	 * Find the latest version among the specified versions that is supported by these API
	 * versions.
	 * @param others the versions to check against
	 * @return the version
	 */
	ApiVersion findLatestSupported(String... others) {
		for (int versionsIndex = this.apiVersions.length - 1; versionsIndex >= 0; versionsIndex--) {
			ApiVersion apiVersion = this.apiVersions[versionsIndex];
			for (int otherIndex = others.length - 1; otherIndex >= 0; otherIndex--) {
				ApiVersion other = ApiVersion.parse(others[otherIndex]);
				if (apiVersion.supports(other)) {
					return apiVersion;
				}
			}
		}
		throw new IllegalStateException(
				"Detected platform API versions '" + StringUtils.arrayToCommaDelimitedString(others)
						+ "' are not included in supported versions '" + this + "'");
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
		return new ApiVersions(Arrays.stream(values).map(ApiVersion::parse).toArray(ApiVersion[]::new));
	}

	static ApiVersions of(int major, IntStream minorsInclusive) {
		return new ApiVersions(
				minorsInclusive.mapToObj((minor) -> ApiVersion.of(major, minor)).toArray(ApiVersion[]::new));
	}

}
