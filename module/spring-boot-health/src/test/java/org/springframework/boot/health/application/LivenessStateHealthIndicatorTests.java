/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LivenessStateHealthIndicator}
 *
 * @author Brian Clozel
 */
class LivenessStateHealthIndicatorTests {

	private ApplicationAvailability availability;

	private LivenessStateHealthIndicator healthIndicator;

	@BeforeEach
	void setUp() {
		this.availability = mock(ApplicationAvailability.class);
		this.healthIndicator = new LivenessStateHealthIndicator(this.availability);
	}

	@Test
	void livenessIsLive() {
		given(this.availability.getLivenessState()).willReturn(LivenessState.CORRECT);
		assertThat(this.healthIndicator.health().getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void livenessIsBroken() {
		given(this.availability.getLivenessState()).willReturn(LivenessState.BROKEN);
		assertThat(this.healthIndicator.health().getStatus()).isEqualTo(Status.DOWN);
	}

}
