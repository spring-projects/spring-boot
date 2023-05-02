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

package org.springframework.boot.docker.compose.core;

import org.springframework.util.Assert;

/**
 * A Docker image name of the form {@literal "docker.io/library/ubuntu"}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ImageName {

	private static final String DEFAULT_DOMAIN = "docker.io";

	private static final String OFFICIAL_REPOSITORY_NAME = "library";

	private static final String LEGACY_DOMAIN = "index.docker.io";

	private final String domain;

	private final String name;

	private final String string;

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
	String getDomain() {
		return this.domain;
	}

	/**
	 * Return the name of this image.
	 * @return the image name
	 */
	String getName() {
		return this.name;
	}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.domain.hashCode();
		result = prime * result + this.name.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return this.string;
	}

	private String getDomainOrDefault(String domain) {
		if (domain == null || LEGACY_DOMAIN.equals(domain)) {
			return DEFAULT_DOMAIN;
		}
		return domain;
	}

	private String getNameWithDefaultPath(String domain, String name) {
		if (DEFAULT_DOMAIN.equals(domain) && !name.contains("/")) {
			return OFFICIAL_REPOSITORY_NAME + "/" + name;
		}
		return name;
	}

	static String parseDomain(String value) {
		int firstSlash = value.indexOf('/');
		String candidate = (firstSlash != -1) ? value.substring(0, firstSlash) : null;
		if (candidate != null && Regex.DOMAIN.matcher(candidate).matches()) {
			return candidate;
		}
		return null;
	}

	static ImageName of(String value) {
		Assert.hasText(value, "Value must not be empty");
		String domain = parseDomain(value);
		String path = (domain != null) ? value.substring(domain.length() + 1) : value;
		Assert.isTrue(Regex.PATH.matcher(path).matches(),
				() -> "Unable to parse name \"" + value + "\". "
						+ "Image name must be in the form '[domainHost:port/][path/]name', "
						+ "with 'path' and 'name' containing only [a-z0-9][.][_][-]");
		return new ImageName(domain, path);
	}

}
