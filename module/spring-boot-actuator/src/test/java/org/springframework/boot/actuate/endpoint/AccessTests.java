/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Access}.
 *
 * @author Phillip Webb
 */
class AccessTests {

	@Test
	void capWhenAboveMaximum() {
		assertThat(Access.UNRESTRICTED.cap(Access.READ_ONLY)).isEqualTo(Access.READ_ONLY);
	}

	@Test
	void capWhenAtMaximum() {
		assertThat(Access.READ_ONLY.cap(Access.READ_ONLY)).isEqualTo(Access.READ_ONLY);
	}

	@Test
	void capWhenBelowMaximum() {
		assertThat(Access.NONE.cap(Access.READ_ONLY)).isEqualTo(Access.NONE);
	}

}
