/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PathPattern}.
 *
 * @author Phillip Webb
 */
class PathPatternTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PathPattern.of(null))
			.withMessage("'path' must not be empty");
	}

	@Test
	void ofWhenEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PathPattern.of(""))
			.withMessage("'path' must not be empty");
	}

	@Test
	void whenEndsWithNoSlash() {
		PathPattern pathPattern = PathPattern.of("a/test");
		assertThat(pathPattern.withoutTrailingSlash()).isEqualTo("a/test");
		assertThat(pathPattern.withTrailingSlash()).isEqualTo("a/test/");
		assertThat(pathPattern).hasToString("a/test");
	}

	@Test
	void whenEndsWithSingleSlash() {
		PathPattern pathPattern = PathPattern.of("a/test/");
		assertThat(pathPattern.withoutTrailingSlash()).isEqualTo("a/test");
		assertThat(pathPattern.withTrailingSlash()).isEqualTo("a/test/");
		assertThat(pathPattern).hasToString("a/test");
	}

	@Test
	void whenEndsWithMultipleSlashes() {
		PathPattern pathPattern = PathPattern.of("a/test//");
		assertThat(pathPattern.withoutTrailingSlash()).isEqualTo("a/test");
		assertThat(pathPattern.withTrailingSlash()).isEqualTo("a/test/");
		assertThat(pathPattern).hasToString("a/test");
	}

}
