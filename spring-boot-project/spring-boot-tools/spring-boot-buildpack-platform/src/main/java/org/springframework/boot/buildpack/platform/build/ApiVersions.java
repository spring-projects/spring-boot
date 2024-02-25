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
	static final ApiVersions SUPPORTED_PLATFORMS = ApiVersions.of(0, IntStream.rangeClosed(3, 12));

	private final ApiVersion[] apiVersions;

	/**
	 * Constructs a new instance of the ApiVersions class with the specified versions.
	 * @param versions the versions to be set for the ApiVersions instance
	 */
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

	/**
	 * Compares this ApiVersions object with the specified object for equality.
	 * @param obj the object to compare with
	 * @return true if the specified object is equal to this ApiVersions object, false
	 * otherwise
	 */
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

	/**
	 * Returns the hash code value for the array of API versions.
	 * @return the hash code value for the array of API versions
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.apiVersions);
	}

	/**
	 * Returns a string representation of the array of API versions.
	 * @return a comma-delimited string representation of the API versions
	 */
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

	/**
	 * Creates an instance of {@link ApiVersions} with the specified major version and a
	 * range of minor versions.
	 * @param major the major version number
	 * @param minorsInclusive a stream of minor versions (inclusive)
	 * @return an instance of {@link ApiVersions} with the specified major and minor
	 * versions
	 */
	static ApiVersions of(int major, IntStream minorsInclusive) {
		return new ApiVersions(
				minorsInclusive.mapToObj((minor) -> ApiVersion.of(major, minor)).toArray(ApiVersion[]::new));
	}

}
