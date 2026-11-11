/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.types;

import org.jspecify.annotations.Nullable;

/**
 * Sample object used for tests.
 *
 * @author Paul Aly
 */
public class NameAndCareer extends Name {

	private final @Nullable String career;

	public NameAndCareer(@Nullable String name, @Nullable String career) {
		super(name);
		this.career = career;
	}

	public @Nullable String getCareer() {
		return this.career;
	}

}
