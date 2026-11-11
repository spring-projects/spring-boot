/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.types;

import org.jspecify.annotations.Nullable;

/**
 * Sample object used for tests.
 *
 * @author Paul Aly
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
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
