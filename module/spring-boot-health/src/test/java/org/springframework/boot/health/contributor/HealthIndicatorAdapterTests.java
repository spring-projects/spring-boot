/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthIndicatorAdapter}.
 *
 * @author Stephane Nicoll
 */
class HealthIndicatorAdapterTests {

	@Test
	void delegateReturnsHealth() {
		HealthIndicator delegate = mock(HealthIndicator.class);
		HealthIndicatorAdapter adapter = new HealthIndicatorAdapter(delegate);
		Health status = Health.up().build();
		given(delegate.health()).willReturn(status);
		StepVerifier.create(adapter.health()).expectNext(status).expectComplete().verify(Duration.ofSeconds(30));
	}

	@Test
	void delegateThrowError() {
		HealthIndicator delegate = mock(HealthIndicator.class);
		HealthIndicatorAdapter adapter = new HealthIndicatorAdapter(delegate);
		given(delegate.health()).willThrow(new IllegalStateException("Expected"));
		StepVerifier.create(adapter.health()).expectError(IllegalStateException.class).verify(Duration.ofSeconds(10));
	}

	@Test
	void delegateRunsOnTheElasticScheduler() {
		String currentThread = Thread.currentThread().getName();
		HealthIndicator delegate = () -> Health
			.status(Thread.currentThread().getName().equals(currentThread) ? Status.DOWN : Status.UP)
			.build();
		HealthIndicatorAdapter adapter = new HealthIndicatorAdapter(delegate);
		StepVerifier.create(adapter.health())
			.expectNext(Health.status(Status.UP).build())
			.expectComplete()
			.verify(Duration.ofSeconds(30));
	}

}
