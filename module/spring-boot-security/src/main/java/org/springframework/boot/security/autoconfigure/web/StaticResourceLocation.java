/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Common locations for static resources.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public enum StaticResourceLocation {

	/**
	 * Resources under {@code "/css"}.
	 */
	CSS("/css/**"),

	/**
	 * Resources under {@code "/js"}.
	 */
	JAVA_SCRIPT("/js/**"),

	/**
	 * Resources under {@code "/images"}.
	 */
	IMAGES("/images/**"),

	/**
	 * Resources under {@code "/webjars"}.
	 */
	WEB_JARS("/webjars/**"),

	/**
	 * The {@code "favicon.ico"} resource and resources matching
	 * <code>"/icons/icon-*"</code>.
	 */
	FAVICON("/favicon.*", "/icons/icon-*"),

	/**
	 * Resources under {@code "/fonts"}.
	 */
	FONTS("/fonts/**");

	private final String[] patterns;

	StaticResourceLocation(String... patterns) {
		this.patterns = patterns;
	}

	public Stream<String> getPatterns() {
		return Arrays.stream(this.patterns);
	}

}
