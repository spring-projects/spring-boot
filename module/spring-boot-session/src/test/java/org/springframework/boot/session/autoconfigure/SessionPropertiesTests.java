/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.autoconfigure;

import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SessionProperties}.
 *
 * @author Stephane Nicoll
 */
class SessionPropertiesTests {

	private final SessionProperties properties = new SessionProperties();

	@Test
	@SuppressWarnings({ "unchecked", "removal" })
	@Deprecated(since = "4.0.1", forRemoval = true)
	void determineTimeoutWithTimeoutIgnoreFallback() {
		this.properties.setTimeout(Duration.ofMinutes(1));
		Supplier<Duration> fallback = mock(Supplier.class);
		assertThat(this.properties.determineTimeout(fallback)).isEqualTo(Duration.ofMinutes(1));
		then(fallback).shouldHaveNoInteractions();
	}

	@Test
	@SuppressWarnings("removal")
	@Deprecated(since = "4.0.1", forRemoval = true)
	void determineTimeoutWithNoTimeoutUseFallback() {
		this.properties.setTimeout(null);
		Duration fallback = Duration.ofMinutes(2);
		assertThat(this.properties.determineTimeout(() -> fallback)).isSameAs(fallback);
	}

	@Test
	void defaultFilterOrderIsCloseToHighestPrecedence() {
		assertThat(this.properties.getServlet().getFilterOrder()).isCloseTo(Ordered.HIGHEST_PRECEDENCE, within(50));
	}

}
