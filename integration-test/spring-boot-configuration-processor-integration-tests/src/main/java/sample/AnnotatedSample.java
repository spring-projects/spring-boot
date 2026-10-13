/*
 * Copyright 2012-present the original author or authors.
 */

package sample;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Test that a valid type is generated if an annotation is present.
 *
 * @author Stephane Nicoll
 * @since 1.5.10
 */
@ConfigurationProperties("annotated")
public class AnnotatedSample {

	/**
	 * A valid name.
	 */
	private @Nullable String name;

	@Valid
	public @Nullable String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

}
