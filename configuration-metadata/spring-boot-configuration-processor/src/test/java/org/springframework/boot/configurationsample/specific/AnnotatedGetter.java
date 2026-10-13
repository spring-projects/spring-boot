/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.specific;

import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.configurationsample.TestConfigurationProperties;

/**
 * An annotated getter with {@code NotEmpty} that triggers a different class type in the
 * compiler. See #11512
 *
 * @author Stephane Nicoll
 */
@TestConfigurationProperties("specific")
public class AnnotatedGetter {

	private String name;

	@NotEmpty
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
