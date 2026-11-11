/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.gradle.testkit;

/**
 * The DSLs supported by Gradle and demonstrated in the documentation samples.
 *
 * @author Andy Wilkinson
 */
public enum Dsl {

	/**
	 * Supported DSL variants.
	 */
	GROOVY("Groovy", ".gradle"), KOTLIN("Kotlin", ".gradle.kts");

	private final String name;

	private final String extension;

	Dsl(String name, String extension) {
		this.name = name;
		this.extension = extension;
	}

	public String getName() {
		return this.name;
	}

	String getExtension() {
		return this.extension;
	}

}
