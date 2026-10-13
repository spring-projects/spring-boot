/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroupsPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * {@link HealthEndpointGroupsPostProcessor} to add
 * {@link AvailabilityProbesHealthEndpointGroups}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class AvailabilityProbesHealthEndpointGroupsPostProcessor implements HealthEndpointGroupsPostProcessor {

	private final boolean addAdditionalPaths;

	AvailabilityProbesHealthEndpointGroupsPostProcessor(Environment environment) {
		this.addAdditionalPaths = "true"
			.equalsIgnoreCase(environment.getProperty("management.endpoint.health.probes.add-additional-paths"));
	}

	@Override
	public HealthEndpointGroups postProcessHealthEndpointGroups(HealthEndpointGroups groups) {
		return new AvailabilityProbesHealthEndpointGroups(groups, this.addAdditionalPaths);
	}

}
