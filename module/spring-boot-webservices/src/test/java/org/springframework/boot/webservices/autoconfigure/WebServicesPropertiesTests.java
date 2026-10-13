/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.autoconfigure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link WebServicesProperties}.
 *
 * @author Madhura Bhave
 */
class WebServicesPropertiesTests {

	private WebServicesProperties properties;

	@BeforeEach
	void setUp() {
		this.properties = new WebServicesProperties();
	}

	@Test
	void pathMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.properties.setPath(""))
			.withMessageContaining("'path' must have length greater than 1");
	}

	@Test
	void pathMustHaveLengthGreaterThanOne() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.properties.setPath("/"))
			.withMessageContaining("'path' must have length greater than 1");
	}

	@Test
	void customPathMustBeginWithASlash() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.properties.setPath("custom"))
			.withMessageContaining("'path' must start with '/'");
	}

}
