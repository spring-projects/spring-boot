/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A lifecycle version number comprised of a major, minor and patch value.
 *
 * @author Phillip Webb
 */
class LifecycleVersion implements Comparable<LifecycleVersion> {

	private static final Comparator<LifecycleVersion> COMPARATOR = Comparator.comparingInt(LifecycleVersion::getMajor)
		.thenComparingInt(LifecycleVersion::getMinor)
		.thenComparing(LifecycleVersion::getPatch);

	private final int major;

	private final int minor;

	private final int patch;

	LifecycleVersion(int major, int minor, int patch) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		LifecycleVersion other = (LifecycleVersion) obj;
		boolean result = true;
		result = result && this.major == other.major;
		result = result && this.minor == other.minor;
		result = result && this.patch == other.patch;
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.major;
		result = prime * result + this.minor;
		result = prime * result + this.patch;
		return result;
	}

	@Override
	public String toString() {
		return "v" + this.major + "." + this.minor + "." + this.patch;
	}

	/**
	 * Return if this version is greater than or equal to the specified version.
	 * @param other the version to compare
	 * @return {@code true} if this version is greater than or equal to the specified
	 * version
	 */
	boolean isEqualOrGreaterThan(LifecycleVersion other) {
		return compareTo(other) >= 0;
	}

	@Override
	public int compareTo(LifecycleVersion other) {
		return COMPARATOR.compare(this, other);
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
	 * Return the patch version number.
	 * @return the patch version
	 */
	int getPatch() {
		return this.patch;
	}

	/**
	 * Factory method to parse a string into a {@link LifecycleVersion} instance.
	 * @param value the value to parse.
	 * @return the corresponding {@link LifecycleVersion}
	 * @throws IllegalArgumentException if the value could not be parsed
	 */
	static LifecycleVersion parse(String value) {
		Assert.hasText(value, "'value' must not be empty");
		String withoutPrefix = (value.startsWith("v") || value.startsWith("V")) ? value.substring(1) : value;
		String[] components = withoutPrefix.split("\\.");
		Assert.isTrue(components.length <= 3, () -> "'value' [%s] must be a valid version number".formatted(value));
		int[] versions = new int[3];
		for (int i = 0; i < components.length; i++) {
			try {
				versions[i] = Integer.parseInt(components[i]);
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException("'value' [" + value + "] must be a valid version number", ex);
			}
		}
		return new LifecycleVersion(versions[0], versions[1], versions[2]);
	}

}
