/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.h2console.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link H2ConsoleProperties}.
 *
 * @author Madhura Bhave
 */
class H2ConsolePropertiesTests {

	@Test
	void pathMustNotBeEmpty() {
		H2ConsoleProperties properties = new H2ConsoleProperties();
		assertThatIllegalArgumentException().isThrownBy(() -> properties.setPath(""))
			.withMessageContaining("'path' must have length greater than 1");
	}

	@Test
	void pathMustHaveLengthGreaterThanOne() {
		H2ConsoleProperties properties = new H2ConsoleProperties();
		assertThatIllegalArgumentException().isThrownBy(() -> properties.setPath("/"))
			.withMessageContaining("'path' must have length greater than 1");
	}

	@Test
	void customPathMustBeginWithASlash() {
		H2ConsoleProperties properties = new H2ConsoleProperties();
		assertThatIllegalArgumentException().isThrownBy(() -> properties.setPath("custom"))
			.withMessageContaining("'path' must start with '/'");
	}

}
