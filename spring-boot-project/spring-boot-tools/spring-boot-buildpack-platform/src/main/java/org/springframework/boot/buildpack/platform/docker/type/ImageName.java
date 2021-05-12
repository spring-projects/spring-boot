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

package org.springframework.boot.buildpack.platform.docker.type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final Pattern PATTERN = Regex.IMAGE_NAME.compile();

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

	public String toLegacyString() {
		if (DEFAULT_DOMAIN.equals(this.domain)) {
			return LEGACY_DOMAIN + "/" + this.name;
		}
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
		Matcher matcher = PATTERN.matcher(value);
		Assert.isTrue(matcher.matches(),
				() -> "Unable to parse name \"" + value + "\". "
						+ "Image name must be in the form '[domainHost:port/][path/]name', "
						+ "with 'path' and 'name' containing only [a-z0-9][.][_][-]");
		return new ImageName(matcher.group("domain"), matcher.group("path"));
	}

}
