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

package org.springframework.boot.buildpack.platform.docker.type;

import org.springframework.util.Assert;

/**
 * A Docker image name of the form {@literal "docker.io/library/ubuntu"}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 * @see ImageReference
 * @see #of(String)
 */
public class ImageName {

	private static final String DEFAULT_DOMAIN = "docker.io";

	private static final String OFFICIAL_REPOSITORY_NAME = "library";

	private static final String LEGACY_DOMAIN = "index.docker.io";

	private final String domain;

	private final String name;

	private final String string;

	/**
	 * Constructs an ImageName object with the given domain and path.
	 * @param domain the domain of the image
	 * @param path the path of the image
	 * @throws IllegalArgumentException if the path is empty
	 */
	ImageName(String domain, String path) {
		Assert.hasText(path, "Path must not be empty");
		this.domain = getDomainOrDefault(domain);
		this.name = getNameWithDefaultPath(this.domain, path);
		this.string = this.domain + "/" + this.name;
	}

	/**
	 * Return the domain for this image name.
	 * @return the domain
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * Return the name of this image.
	 * @return the image name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Compares this ImageName object to the specified object for equality.
	 * @param obj the object to compare to
	 * @return true if the specified object is equal to this ImageName object, false
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
		ImageName other = (ImageName) obj;
		boolean result = true;
		result = result && this.domain.equals(other.domain);
		result = result && this.name.equals(other.name);
		return result;
	}

	/**
	 * Returns a hash code value for the ImageName object.
	 * @return the hash code value for the ImageName object
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.domain.hashCode();
		result = prime * result + this.name.hashCode();
		return result;
	}

	/**
	 * Returns a string representation of the ImageName object.
	 * @return the string representation of the ImageName object
	 */
	@Override
	public String toString() {
		return this.string;
	}

	/**
	 * Converts the ImageName object to a legacy string format.
	 * @return the legacy string representation of the ImageName object
	 */
	public String toLegacyString() {
		if (DEFAULT_DOMAIN.equals(this.domain)) {
			return LEGACY_DOMAIN + "/" + this.name;
		}
		return this.string;
	}

	/**
	 * Returns the specified domain if it is not null and not equal to the legacy domain.
	 * If the domain is null or equal to the legacy domain, the default domain is
	 * returned.
	 * @param domain the domain to be checked
	 * @return the specified domain if it is valid, otherwise the default domain
	 */
	private String getDomainOrDefault(String domain) {
		if (domain == null || LEGACY_DOMAIN.equals(domain)) {
			return DEFAULT_DOMAIN;
		}
		return domain;
	}

	/**
	 * Returns the name with the default path if the given domain is the default domain
	 * and the name does not contain a path.
	 * @param domain the domain of the image
	 * @param name the name of the image
	 * @return the name with the default path if applicable, otherwise the original name
	 */
	private String getNameWithDefaultPath(String domain, String name) {
		if (DEFAULT_DOMAIN.equals(domain) && !name.contains("/")) {
			return OFFICIAL_REPOSITORY_NAME + "/" + name;
		}
		return name;
	}

	/**
	 * Create a new {@link ImageName} from the given value. The following value forms can
	 * be used:
	 * <ul>
	 * <li>{@code name} (maps to {@code docker.io/library/name})</li>
	 * <li>{@code domain/name}</li>
	 * <li>{@code domain:port/name}</li>
	 * </ul>
	 * @param value the value to parse
	 * @return an {@link ImageName} instance
	 */
	public static ImageName of(String value) {
		Assert.hasText(value, "Value must not be empty");
		String domain = parseDomain(value);
		String path = (domain != null) ? value.substring(domain.length() + 1) : value;
		Assert.isTrue(Regex.PATH.matcher(path).matches(),
				() -> "Unable to parse name \"" + value + "\". "
						+ "Image name must be in the form '[domainHost:port/][path/]name', "
						+ "with 'path' and 'name' containing only [a-z0-9][.][_][-]");
		return new ImageName(domain, path);
	}

	/**
	 * Parses the domain from the given value.
	 * @param value the value to parse
	 * @return the parsed domain, or null if the value does not contain a valid domain
	 */
	static String parseDomain(String value) {
		int firstSlash = value.indexOf('/');
		String candidate = (firstSlash != -1) ? value.substring(0, firstSlash) : null;
		if (candidate != null && Regex.DOMAIN.matcher(candidate).matches()) {
			return candidate;
		}
		return null;
	}

}
