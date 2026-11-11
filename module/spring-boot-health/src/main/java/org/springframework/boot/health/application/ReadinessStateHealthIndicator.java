/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.application;

import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

/**
 * A {@link HealthIndicator} that checks the {@link ReadinessState} of the application.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 4.0.0
 */
public class ReadinessStateHealthIndicator extends AvailabilityStateHealthIndicator {

	public ReadinessStateHealthIndicator(ApplicationAvailability availability) {
		super(availability, ReadinessState.class, (statusMappings) -> {
			statusMappings.add(ReadinessState.ACCEPTING_TRAFFIC, Status.UP);
			statusMappings.add(ReadinessState.REFUSING_TRAFFIC, Status.OUT_OF_SERVICE);
		});
	}

	@Override
	protected AvailabilityState getState(ApplicationAvailability applicationAvailability) {
		return applicationAvailability.getReadinessState();
	}

}
