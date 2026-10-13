/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import org.springframework.util.Assert;

/**
 * Identifying information about the tooling that created a builder.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class Creator {

	private final String version;

	Creator(String version) {
		this.version = version;
	}

	/**
	 * Return the name of the builder creator.
	 * @return the name
	 */
	public String getName() {
		return "Spring Boot";
	}

	/**
	 * Return the version of the builder creator.
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * Create a new {@code Creator} using the provided version.
	 * @param version the creator version
	 * @return a new creator instance
	 */
	public static Creator withVersion(String version) {
		Assert.notNull(version, "'version' must not be null");
		return new Creator(version);
	}

	@Override
	public String toString() {
		return getName() + " version " + getVersion();
	}

}
