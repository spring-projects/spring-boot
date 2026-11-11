/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link SessionProperties}.
 *
 * @author Stephane Nicoll
 */
class SessionPropertiesTests {

	private final SessionProperties properties = new SessionProperties();

	@Test
	void defaultFilterOrderIsCloseToHighestPrecedence() {
		assertThat(this.properties.getServlet().getFilterOrder()).isCloseTo(Ordered.HIGHEST_PRECEDENCE, within(50));
	}

}
