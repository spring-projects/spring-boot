/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.jspecify.annotations.Nullable;

/**
 * A {@link ConfigurationProperties @ConfigurationProperties} with an additional
 * single-arg public constructor. Used in {@link ConfigurationPropertiesTests}.
 *
 * @author Madhura Bhave
 */
@ConfigurationProperties("test")
public class WithPublicStringConstructorProperties {

	private @Nullable String a;

	public WithPublicStringConstructorProperties() {
	}

	public WithPublicStringConstructorProperties(String a) {
		this.a = a;
	}

	public @Nullable String getA() {
		return this.a;
	}

	public void setA(@Nullable String a) {
		this.a = a;
	}

}
