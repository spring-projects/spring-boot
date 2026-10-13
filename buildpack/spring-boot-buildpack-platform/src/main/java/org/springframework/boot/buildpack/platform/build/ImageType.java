/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

/**
 * Image types.
 *
 * @author Andrey Shlykov
 */
enum ImageType {

	/**
	 * Builder image.
	 */
	BUILDER("builder image"),

	/**
	 * Run image.
	 */
	RUNNER("run image"),

	/**
	 * Buildpack image.
	 */
	BUILDPACK("buildpack image");

	private final String description;

	ImageType(String description) {
		this.description = description;
	}

	String getDescription() {
		return this.description;
	}

}
