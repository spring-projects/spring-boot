/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure.health;

import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponent;
import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembership;

/**
 * Auto-configured {@link HealthCheckedGrpcComponent}.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthCheckedGrpcComponent implements HealthCheckedGrpcComponent {

	private final HealthContributorMembership membership;

	private final StatusAggregator statusAggregator;

	private final StatusMapper statusMapper;

	AutoConfiguredHealthCheckedGrpcComponent(HealthContributorMembership membership, StatusAggregator statusAggregator,
			StatusMapper statusMapper) {
		this.membership = membership;
		this.statusAggregator = statusAggregator;
		this.statusMapper = statusMapper;
	}

	@Override
	public boolean isMember(String healthContributorName) {
		return this.membership.isMember(healthContributorName);
	}

	@Override
	public StatusAggregator getStatusAggregator() {
		return this.statusAggregator;
	}

	@Override
	public StatusMapper getStatusMapper() {
		return this.statusMapper;
	}

}
