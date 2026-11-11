/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.application;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

/**
 * A {@link HealthIndicator} that checks the {@link LivenessState} of the application.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public class LivenessStateHealthIndicator extends AvailabilityStateHealthIndicator {

	public LivenessStateHealthIndicator(ApplicationAvailability availability) {
		super(availability, LivenessState.class, (statusMappings) -> {
			statusMappings.add(LivenessState.CORRECT, Status.UP);
			statusMappings.add(LivenessState.BROKEN, Status.DOWN);
		});
	}

	@Override
	protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
		return applicationAvailability.getLivenessState();
	}

}
