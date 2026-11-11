/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.types;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Sample object used for tests.
 *
 * @author Phillip Webb
 * @author Paul Aly
 */
public final class NameAndAge extends Name {

	private final int age;

	private NameAndAge(@Nullable String name, int age) {
		super(name);
		this.age = age;
	}

	public int getAge() {
		return this.age;
	}

	public String asKey() {
		return this.name + " is " + this.age;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof NameAndAge other) {
			boolean rtn = true;
			rtn = rtn && ObjectUtils.nullSafeEquals(this.name, other.name);
			rtn = rtn && ObjectUtils.nullSafeEquals(this.age, other.age);
			return rtn;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.name);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.age);
		return result;
	}

	public static NameAndAge create(@Nullable String name, int age) {
		return new NameAndAge(name, age);
	}

}
