/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

/**
 * Exposes the Spring Boot version for SSL Provider use.
 *
 * @author Phillip Webb
 */
final class SpringBootProviderVersion {

	private SpringBootProviderVersion() {
	}

	/**
	 * Return the full version string of the present Spring Boot codebase.
	 * @return the version of Spring Boot
	 */
	static String get() {
		return "${springBootVersion}";
	}

}
