/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import org.jspecify.annotations.Nullable;

/**
 * Java bean with an additional public single-arg constructor.
 *
 * @author Phillip Webb
 */
public class JavaBeanWithPublicConstructor {

	private @Nullable String value;

	public JavaBeanWithPublicConstructor() {
	}

	public JavaBeanWithPublicConstructor(String value) {
		setValue(value);
	}

	public @Nullable String getValue() {
		return this.value;
	}

	public void setValue(@Nullable String value) {
		this.value = value;
	}

}
