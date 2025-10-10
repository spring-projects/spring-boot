/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * API Version number comprised of a major and minor value.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0.0
 */
public final class ApiVersion {

	private static final Pattern PATTERN = Pattern.compile("^v?(\\d+)\\.(\\d*)$");

	private final int major;

	private final int minor;

	private ApiVersion(int major, int minor) {
		this.major = major;
		this.minor = minor;
	}

	/**
	 * Return the major version number.
	 * @return the major version
	 */
	int getMajor() {
		return this.major;
	}

	/**
	 * Return the minor version number.
	 * @return the minor version
	 */
	int getMinor() {
		return this.minor;
	}

	/**
	 * Returns if this API version supports the given version. A {@code 0.x} matches only
	 * the same version number. A 1.x or higher release matches when the versions have the
	 * same major version and a minor that is equal or greater.
	 * @param other the version to check against
	 * @return if the specified API version is supported
	 */
	public boolean supports(ApiVersion other) {
		if (equals(other)) {
			return true;
		}
		if (this.major == 0 || this.major != other.major) {
			return false;
		}
		return this.minor >= other.minor;
	}

	/**
	 * Returns if this API version supports any of the given versions.
	 * @param others the versions to check against
	 * @return if any of the specified API versions are supported
	 * @see #supports(ApiVersion)
	 */
	public boolean supportsAny(ApiVersion... others) {
		for (ApiVersion other : others) {
			if (supports(other)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ApiVersion other = (ApiVersion) obj;
		return (this.major == other.major) && (this.minor == other.minor);
	}

	@Override
	public int hashCode() {
		return this.major * 31 + this.minor;
	}

	@Override
	public String toString() {
		return this.major + "." + this.minor;
	}

	/**
	 * Factory method to parse a string into an {@link ApiVersion} instance.
	 * @param value the value to parse.
	 * @return the corresponding {@link ApiVersion}
	 * @throws IllegalArgumentException if the value could not be parsed
	 */
	public static ApiVersion parse(String value) {
		Assert.hasText(value, "'value' must not be empty");
		Matcher matcher = PATTERN.matcher(value);
		Assert.isTrue(matcher.matches(),
				() -> "'value' [%s] must contain a well formed version number".formatted(value));
		try {
			int major = Integer.parseInt(matcher.group(1));
			int minor = Integer.parseInt(matcher.group(2));
			return new ApiVersion(major, minor);
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("'value' must contain a well formed version number [" + value + "]", ex);
		}
	}

	public static ApiVersion of(int major, int minor) {
		return new ApiVersion(major, minor);
	}

}
