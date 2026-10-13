/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PingHealthIndicator}.
 *
 * @author Phillip Webb
 */
class PingHealthIndicatorTests {

	@Test
	void indicatesUp() {
		PingHealthIndicator healthIndicator = new PingHealthIndicator();
		assertThat(healthIndicator.health().getStatus()).isEqualTo(Status.UP);
	}

}
