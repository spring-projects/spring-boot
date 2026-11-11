/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

/**
 * Exception thrown when the AOT initializer couldn't be found.
 *
 * @author Moritz Halbritter
 * @since 3.2.6
 */
public class AotInitializerNotFoundException extends RuntimeException {

	private final Class<?> mainClass;

	public AotInitializerNotFoundException(Class<?> mainClass, String initializerClassName) {
		super("Startup with AOT mode enabled failed: AOT initializer %s could not be found"
			.formatted(initializerClassName));
		this.mainClass = mainClass;
	}

	public Class<?> getMainClass() {
		return this.mainClass;
	}

}
