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

package org.springframework.boot.buildpack.platform.docker.type;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A platform specification for a Docker image.
 *
 * @author Scott Frederick
 * @since 3.4.0
 */
public class ImagePlatform {

	private final String os;

	private final @Nullable String architecture;

	private final @Nullable String variant;

	ImagePlatform(String os, @Nullable String architecture, @Nullable String variant) {
		Assert.hasText(os, "'os' must not be empty");
		this.os = os;
		this.architecture = architecture;
		this.variant = variant;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ImagePlatform other = (ImagePlatform) obj;
		return Objects.equals(this.architecture, other.architecture) && Objects.equals(this.os, other.os)
				&& Objects.equals(this.variant, other.variant);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.architecture, this.os, this.variant);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(this.os);
		if (this.architecture != null) {
			builder.append("/").append(this.architecture);
		}
		if (this.variant != null) {
			builder.append("/").append(this.variant);
		}
		return builder.toString();
	}

	/**
	 * Create a new {@link ImagePlatform} from the given value in the form
	 * {@code os[/architecture[/variant]]}.
	 * @param value the value to parse
	 * @return an {@link ImagePlatform} instance
	 */
	public static ImagePlatform of(String value) {
		Assert.hasText(value, "'value' must not be empty");
		String[] split = value.split("/+");
		return switch (split.length) {
			case 1 -> new ImagePlatform(split[0], null, null);
			case 2 -> new ImagePlatform(split[0], split[1], null);
			case 3 -> new ImagePlatform(split[0], split[1], split[2]);
			default -> throw new IllegalArgumentException(
					"'value' [" + value + "] must be in the form 'os[/architecture[/variant]]'");
		};
	}

	/**
	 * Create a new {@link ImagePlatform} matching the platform information from the
	 * provided {@link Image}.
	 * @param image the image to get platform information from
	 * @return an {@link ImagePlatform} instance
	 */
	public static ImagePlatform from(Image image) {
		return new ImagePlatform(image.getOs(), image.getArchitecture(), image.getVariant());
	}

	/**
	 * Return the value to use for the Docker API {@code platform} query parameter for the
	 * given API version.
	 *
	 * For API versions that support JSON (1.49 and above), this method returns a JSON
	 * object in the form {@code {"os":"...","architecture":"...","variant":"..."}}.
	 * For lower API versions, the legacy string representation {@code os[/arch[/variant]]}
	 * is returned.
	 * @param apiVersion the Docker API version to target
	 * @return the query parameter value to use for {@code platform}
	 */
	public String toQueryParameter(ApiVersion apiVersion) {
		Assert.notNull(apiVersion, "'apiVersion' must not be null");
		if (apiVersion.supports(ApiVersion.of(1, 49))) {
			StringBuilder json = new StringBuilder("{");
			json.append("\"os\":\"").append(this.os).append("\"");
			if (this.architecture != null && !this.architecture.isEmpty()) {
				json.append(",\"architecture\":\"").append(this.architecture).append("\"");
			}
			if (this.variant != null && !this.variant.isEmpty()) {
				json.append(",\"variant\":\"").append(this.variant).append("\"");
			}
			json.append("}");
			return json.toString();
		}
		return toString();
	}
}
