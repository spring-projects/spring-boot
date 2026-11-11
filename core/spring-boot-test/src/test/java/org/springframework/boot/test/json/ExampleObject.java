/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Example object used for serialization.
 */
public class ExampleObject {

	private @Nullable String name;

	private int age;

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

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		ExampleObject other = (ExampleObject) obj;
		return ObjectUtils.nullSafeEquals(this.name, other.name) && ObjectUtils.nullSafeEquals(this.age, other.age);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return this.name + " " + this.age;
	}

}
