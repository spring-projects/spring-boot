/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import com.fasterxml.jackson.annotation.JsonView;
import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Example object used for serialization/deserialization with view.
 *
 * @author Madhura Bhave
 */
public class ExampleObjectWithView {

	@JsonView(TestView.class)
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
		ExampleObjectWithView other = (ExampleObjectWithView) obj;
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

	static class TestView {

	}

}
