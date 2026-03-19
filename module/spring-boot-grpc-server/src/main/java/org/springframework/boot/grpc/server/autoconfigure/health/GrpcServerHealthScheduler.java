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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import io.grpc.protobuf.services.HealthStatusManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.grpc.server.health.GrpcServerHealth;
import org.springframework.context.ApplicationListener;
import org.springframework.core.log.LogMessage;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.function.SingletonSupplier;

/**
 * Schedules gRPC health updates one the gRPC server has been started.
 *
 * @author Phillip Webb
 * @author Chris Bono
 */
class GrpcServerHealthScheduler implements ApplicationListener<GrpcServerStartedEvent> {

	private static final Log logger = LogFactory.getLog(GrpcServerHealthScheduler.class);

	private final SingletonSupplier<ScheduledFuture<?>> scheduleHealth;

	GrpcServerHealthScheduler(GrpcServerHealth grpcServerHealth, HealthStatusManager grpcServerHealthStatusManager,
			TaskScheduler taskScheduler, Duration period, Duration delay) {
		this(Clock.systemDefaultZone(), grpcServerHealth, grpcServerHealthStatusManager, taskScheduler, period, delay);
	}

	GrpcServerHealthScheduler(Clock clock, GrpcServerHealth grpcServerHealth,
			HealthStatusManager grpcServerHealthStatusManager, TaskScheduler taskScheduler, Duration period,
			Duration delay) {
		this.scheduleHealth = SingletonSupplier.of(() -> {
			logger.debug(LogMessage
				.of(() -> "Scheduling gRPC server health updates every %s seconds (after a delay of %s seconds)"
					.formatted((period.toMillis() / 1000.0), delay.toMillis() / 1000.0)));
			Runnable task = () -> grpcServerHealth.update(grpcServerHealthStatusManager);
			return taskScheduler.scheduleAtFixedRate(task, Instant.now(clock).plus(delay), period);
		});
	}

	@Override
	public void onApplicationEvent(GrpcServerStartedEvent event) {
		this.scheduleHealth.get();
	}

}
