/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RemoteIp}.
 *
 * @author Phillip Webb
 */
class RemoteIpTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenUsageIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new RemoteIp(null, null, null))
			.withMessage("'usage' must not be null");

	}

}
