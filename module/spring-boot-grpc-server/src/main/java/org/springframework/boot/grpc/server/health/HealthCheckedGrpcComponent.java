/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.health;

/**
 * A single gRPC component that can be checked for health.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
public interface HealthCheckedGrpcComponent {

	/**
	 * Returns {@code true} if the given health contributor is a member of this component.
	 * @param healthContributorName the contributor name
	 * @return {@code true} if the contributor is a member
	 */
	boolean isMember(String healthContributorName);

	/**
	 * Returns the status aggregator that should be used for this component.
	 * @return the status aggregator
	 */
	StatusAggregator getStatusAggregator();

	/**
	 * Returns the {@link StatusMapper} that should be used for this component.
	 * @return the status mapper
	 */
	StatusMapper getStatusMapper();

}
