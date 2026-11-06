/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.ForwardedRequestHeaders.Naming;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ForwardedRequestHeaders}.
 *
 * @author Phillip Webb
 */
class ForwardedRequestHeadersTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenNamingIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ForwardedRequestHeaders(null, false))
			.withMessage("'naming' must not be null");
	}

	@Test
	void createWhenUsePrefixWithoutXForwardedNamingThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> new ForwardedRequestHeaders(Naming.RFC_7239, true))
			.withMessage("'usePrefix' can only be used with 'x-forwarded' naming");
	}

}
