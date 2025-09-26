/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.grpc.server.autoconfigure.health;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.core.log.LogAccessor;
import org.springframework.util.Assert;

/**
 * Adapts {@link HealthIndicator Actuator health indicators} into gRPC health checks by
 * periodically invoking {@link HealthEndpoint health endpoints} and updating the health
 * status in gRPC {@link HealthStatusManager}.
 *
 * @author Chris Bono
 * @since 4.0.0
 */
public class ActuatorHealthAdapter {

	private static final String INVALID_INDICATOR_MSG = "Unable to determine health for '%s' - check that your configured health-indicator-paths point to available indicators";

	private final LogAccessor logger = new LogAccessor(getClass());

	private final HealthStatusManager healthStatusManager;

	private final HealthEndpoint healthEndpoint;

	private final StatusAggregator statusAggregator;

	private final boolean updateOverallHealth;

	private final List<String> healthIndicatorPaths;

	protected ActuatorHealthAdapter(HealthStatusManager healthStatusManager, HealthEndpoint healthEndpoint,
			StatusAggregator statusAggregator, boolean updateOverallHealth, List<String> healthIndicatorPaths) {
		this.healthStatusManager = healthStatusManager;
		this.healthEndpoint = healthEndpoint;
		this.statusAggregator = statusAggregator;
		this.updateOverallHealth = updateOverallHealth;
		Assert.notEmpty(healthIndicatorPaths, () -> "at least one health indicator path is required");
		this.healthIndicatorPaths = healthIndicatorPaths;
	}

	protected void updateHealthStatus() {
		var individualStatuses = this.updateIndicatorsHealthStatus();
		if (this.updateOverallHealth) {
			this.updateOverallHealthStatus(individualStatuses);
		}
	}

	protected Set<Status> updateIndicatorsHealthStatus() {
		Set<Status> statuses = new HashSet<>();
		this.healthIndicatorPaths.forEach((healthIndicatorPath) -> {
			var healthComponent = this.healthEndpoint.healthForPath(healthIndicatorPath.split("/"));
			if (healthComponent == null) {
				this.logger.warn(() -> INVALID_INDICATOR_MSG.formatted(healthIndicatorPath));
			}
			else {
				this.logger.trace(() -> "Actuator returned '%s' for indicator '%s'".formatted(healthComponent,
						healthIndicatorPath));
				var actuatorStatus = healthComponent.getStatus();
				var grpcStatus = toServingStatus(actuatorStatus.getCode());
				this.healthStatusManager.setStatus(healthIndicatorPath, grpcStatus);
				this.logger.trace(() -> "Updated gRPC health status to '%s' for service '%s'".formatted(grpcStatus,
						healthIndicatorPath));
				statuses.add(actuatorStatus);
			}
		});
		return statuses;
	}

	protected void updateOverallHealthStatus(Set<Status> individualStatuses) {
		var overallActuatorStatus = this.statusAggregator.getAggregateStatus(individualStatuses);
		var overallGrpcStatus = toServingStatus(overallActuatorStatus.getCode());
		this.logger.trace(() -> "Actuator aggregate status '%s' for overall health".formatted(overallActuatorStatus));
		this.healthStatusManager.setStatus("", overallGrpcStatus);
		this.logger.trace(() -> "Updated overall gRPC health status to '%s'".formatted(overallGrpcStatus));
	}

	protected ServingStatus toServingStatus(String actuatorHealthStatusCode) {
		return switch (actuatorHealthStatusCode) {
			case "UP" -> ServingStatus.SERVING;
			case "DOWN" -> ServingStatus.NOT_SERVING;
			case "OUT_OF_SERVICE" -> ServingStatus.NOT_SERVING;
			case "UNKNOWN" -> ServingStatus.UNKNOWN;
			default -> ServingStatus.UNKNOWN;
		};
	}

}
