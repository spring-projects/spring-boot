/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.type;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A reference to a Docker container.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public final class ContainerReference {

	private final String value;

	private ContainerReference(String value) {
		Assert.hasText(value, "'value' must not be empty");
		this.value = value;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ContainerReference other = (ContainerReference) obj;
		return this.value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Factory method to create a {@link ContainerReference} with a specific value.
	 * @param value the container reference value
	 * @return a new container reference instance
	 */
	public static ContainerReference of(String value) {
		return new ContainerReference(value);
	}

}
