/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

/**
 * Exposes the Spring Boot version.
 *
 * @author Drummond Dawson
 * @author Hendrig Sellik
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 1.3.0
 */
public final class SpringBootVersion {

	private SpringBootVersion() {
	}

	/**
	 * Return the full version string of the present Spring Boot codebase.
	 * @return the version of Spring Boot
	 */
	public static String getVersion() {
		return "${springBootVersion}";
	}

}
