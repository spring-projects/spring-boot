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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A reference to a Docker image of the form {@code "imagename[:tag|@digest]"}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 * @see ImageName
 */
public final class ImageReference {

	private static final Pattern JAR_VERSION_PATTERN = Pattern.compile("^(.*)(\\-\\d+)$");

	private static final String LATEST = "latest";

	private final ImageName name;

	private final String tag;

	private final String digest;

	private final String string;

	private ImageReference(ImageName name, String tag, String digest) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.tag = tag;
		this.digest = digest;
		this.string = buildString(name.toString(), tag, digest);
	}

	/**
	 * Return the domain for this image name.
	 * @return the domain
	 * @see ImageName#getDomain()
	 */
	public String getDomain() {
		return this.name.getDomain();
	}

	/**
	 * Return the name of this image.
	 * @return the image name
	 * @see ImageName#getName()
	 */
	public String getName() {
		return this.name.getName();
	}

	/**
	 * Return the tag from the reference or {@code null}.
	 * @return the referenced tag
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * Return the digest from the reference or {@code null}.
	 * @return the referenced digest
	 */
	public String getDigest() {
		return this.digest;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ImageReference other = (ImageReference) obj;
		boolean result = true;
		result = result && this.name.equals(other.name);
		result = result && ObjectUtils.nullSafeEquals(this.tag, other.tag);
		result = result && ObjectUtils.nullSafeEquals(this.digest, other.digest);
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.name.hashCode();
		result = prime * result + ObjectUtils.nullSafeHashCode(this.tag);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.digest);
		return result;
	}

	@Override
	public String toString() {
		return this.string;
	}

	public String toLegacyString() {
		return buildString(this.name.toLegacyString(), this.tag, this.digest);
	}

	private String buildString(String name, String tag, String digest) {
		StringBuilder string = new StringBuilder(name);
		if (tag != null) {
			string.append(":").append(tag);
		}
		if (digest != null) {
			string.append("@").append(digest);
		}
		return string.toString();
	}

	/**
	 * Create a new {@link ImageReference} with an updated digest.
	 * @param digest the new digest
	 * @return an updated image reference
	 */
	public ImageReference withDigest(String digest) {
		return new ImageReference(this.name, null, digest);
	}

	/**
	 * Return an {@link ImageReference} in the form {@code "imagename:tag"}. If the tag
	 * has not been defined then {@code latest} is used.
	 * @return the image reference in tagged form
	 * @throws IllegalStateException if the image reference contains a digest
	 */
	public ImageReference inTaggedForm() {
		Assert.state(this.digest == null, () -> "Image reference '" + this + "' cannot contain a digest");
		return new ImageReference(this.name, (this.tag != null) ? this.tag : LATEST, null);
	}

	/**
	 * Return an {@link ImageReference} containing either a tag or a digest. If neither
	 * the digest nor the tag has been defined then tag {@code latest} is used.
	 * @return the image reference in tagged or digest form
	 */
	public ImageReference inTaggedOrDigestForm() {
		if (this.digest != null) {
			return this;
		}
		return inTaggedForm();
	}

	/**
	 * Create a new {@link ImageReference} instance deduced from a source JAR file that
	 * follows common Java naming conventions.
	 * @param jarFile the source jar file
	 * @return an {@link ImageName} for the jar file.
	 */
	public static ImageReference forJarFile(File jarFile) {
		String filename = jarFile.getName();
		Assert.isTrue(filename.toLowerCase().endsWith(".jar"), () -> "File '" + jarFile + "' is not a JAR");
		filename = filename.substring(0, filename.length() - 4);
		int firstDot = filename.indexOf('.');
		if (firstDot == -1) {
			return of(filename);
		}
		String name = filename.substring(0, firstDot);
		String version = filename.substring(firstDot + 1);
		Matcher matcher = JAR_VERSION_PATTERN.matcher(name);
		if (matcher.matches()) {
			name = matcher.group(1);
			version = matcher.group(2).substring(1) + "." + version;
		}
		return of(ImageName.of(name), version);
	}

	/**
	 * Generate an image name with a random suffix.
	 * @param prefix the name prefix
	 * @return a random image reference
	 */
	public static ImageReference random(String prefix) {
		return ImageReference.random(prefix, 10);
	}

	/**
	 * Generate an image name with a random suffix.
	 * @param prefix the name prefix
	 * @param randomLength the number of chars in the random part of the name
	 * @return a random image reference
	 */
	public static ImageReference random(String prefix, int randomLength) {
		return of(RandomString.generate(prefix, randomLength));
	}

	/**
	 * Create a new {@link ImageReference} from the given value. The following value forms
	 * can be used:
	 * <ul>
	 * <li>{@code name} (maps to {@code docker.io/library/name})</li>
	 * <li>{@code domain/name}</li>
	 * <li>{@code domain:port/name}</li>
	 * <li>{@code domain:port/name:tag}</li>
	 * <li>{@code domain:port/name@digest}</li>
	 * </ul>
	 * @param value the value to parse
	 * @return an {@link ImageName} instance
	 */
	public static ImageReference of(String value) {
		Assert.hasText(value, "Value must not be null");
		String domain = ImageName.parseDomain(value);
		String path = (domain != null) ? value.substring(domain.length() + 1) : value;
		String digest = null;
		int digestSplit = path.indexOf("@");
		if (digestSplit != -1) {
			String remainder = path.substring(digestSplit + 1);
			Matcher matcher = Regex.DIGEST.matcher(remainder);
			if (matcher.find()) {
				digest = remainder.substring(0, matcher.end());
				remainder = remainder.substring(matcher.end());
				path = path.substring(0, digestSplit) + remainder;
			}
		}
		String tag = null;
		int tagSplit = path.lastIndexOf(":");
		if (tagSplit != -1) {
			String remainder = path.substring(tagSplit + 1);
			Matcher matcher = Regex.TAG.matcher(remainder);
			if (matcher.find()) {
				tag = remainder.substring(0, matcher.end());
				remainder = remainder.substring(matcher.end());
				path = path.substring(0, tagSplit) + remainder;
			}
		}
		Assert.isTrue(Regex.PATH.matcher(path).matches(),
				() -> "Unable to parse image reference \"" + value + "\". "
						+ "Image reference must be in the form '[domainHost:port/][path/]name[:tag][@digest]', "
						+ "with 'path' and 'name' containing only [a-z0-9][.][_][-]");
		ImageName name = new ImageName(domain, path);
		return new ImageReference(name, tag, digest);
	}

	/**
	 * Create a new {@link ImageReference} from the given {@link ImageName}.
	 * @param name the image name
	 * @return a new image reference
	 */
	public static ImageReference of(ImageName name) {
		return new ImageReference(name, null, null);
	}

	/**
	 * Create a new {@link ImageReference} from the given {@link ImageName} and tag.
	 * @param name the image name
	 * @param tag the referenced tag
	 * @return a new image reference
	 */
	public static ImageReference of(ImageName name, String tag) {
		return new ImageReference(name, tag, null);
	}

	/**
	 * Create a new {@link ImageReference} from the given {@link ImageName}, tag and
	 * digest.
	 * @param name the image name
	 * @param tag the referenced tag
	 * @param digest the referenced digest
	 * @return a new image reference
	 */
	public static ImageReference of(ImageName name, String tag, String digest) {
		return new ImageReference(name, tag, digest);
	}

}
