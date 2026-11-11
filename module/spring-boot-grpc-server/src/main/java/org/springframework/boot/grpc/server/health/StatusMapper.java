/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.health;

import java.util.Map;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.Status;
import org.springframework.util.CollectionUtils;

/**
 * Strategy used to map a {@link Status health status} to a gRPC {@link ServingStatus}.
 *
 * @author Phillip Webb
 * @since 4.1.0
 */
@FunctionalInterface
public interface StatusMapper {

	/**
	 * Return the gRPC serving status that corresponds to the given {@link Status health
	 * status}.
	 * @param status the health status to map
	 * @return the corresponding gRPC serving status
	 */
	ServingStatus getServingStatus(Status status);

	/**
	 * Create a new {@link StatusMapper} with the specified mappings.
	 * @param mappings the mappings to use or {@code null} to use the default mappings
	 * @return a {@link StatusMapper} or {@link #getDefault()}
	 */
	static StatusMapper of(@Nullable Map<String, ServingStatus> mappings) {
		return CollectionUtils.isEmpty(mappings) ? SimpleServingStatusMapper.DEFAULT_MAPPINGS
				: new SimpleServingStatusMapper(mappings);
	}

	/**
	 * Return an {@link StatusMapper} instance using default mappings.
	 * @return a mapper using default mappings
	 */
	static StatusMapper getDefault() {
		return SimpleServingStatusMapper.DEFAULT_MAPPINGS;
	}

}
