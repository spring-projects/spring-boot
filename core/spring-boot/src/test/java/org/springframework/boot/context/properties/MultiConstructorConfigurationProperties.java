/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.jspecify.annotations.Nullable;

/**
 * Class used to test multi-constructor binding. Must be public and have public
 * constructors.
 *
 * @author Phillip Webb
 */
public class MultiConstructorConfigurationProperties {

	private @Nullable String name;

	private int age;

	public MultiConstructorConfigurationProperties() {
	}

	public MultiConstructorConfigurationProperties(String name) {
		this.name = name;
	}

	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	public int getAge() {
		return this.age;
	}

	public void setAge(int age) {
		this.age = age;
	}

}
