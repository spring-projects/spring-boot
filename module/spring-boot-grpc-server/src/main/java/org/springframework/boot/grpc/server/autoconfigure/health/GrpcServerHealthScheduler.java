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

import io.grpc.protobuf.services.HealthStatusManager;
import jakarta.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.boot.grpc.server.health.GrpcServerHealth;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.context.WebApplicationContext;

/**
 * Schedules gRPC health updates once the gRPC server has been started.
 *
 * @author Phillip Webb
 * @author Chris Bono
 */
class GrpcServerHealthScheduler {

	private static final Log logger = LogFactory.getLog(GrpcServerHealthScheduler.class);

	GrpcServerHealthScheduler(ConfigurableApplicationContext applicationContext, GrpcServerHealth grpcServerHealth,
			HealthStatusManager grpcServerHealthStatusManager, TaskScheduler taskScheduler, Duration period,
			Duration delay) {
		this(applicationContext, grpcServerHealth, grpcServerHealthStatusManager, taskScheduler, period, delay,
				Clock.systemDefaultZone());
	}

	GrpcServerHealthScheduler(ConfigurableApplicationContext applicationContext, GrpcServerHealth grpcServerHealth,
			HealthStatusManager grpcServerHealthStatusManager, TaskScheduler taskScheduler, Duration period,
			Duration delay, Clock clock) {
		SingletonSupplier<?> scheduleHealth = SingletonSupplier.of(() -> {
			logger.debug(LogMessage
				.of(() -> "Scheduling gRPC server health updates every %s seconds (after a delay of %s seconds)"
					.formatted((period.toMillis() / 1000.0), delay.toMillis() / 1000.0)));
			Runnable task = () -> grpcServerHealth.update(grpcServerHealthStatusManager);
			return taskScheduler.scheduleAtFixedRate(task, Instant.now(clock).plus(delay), period);
		});
		if (ClassUtils.isPresent("jakarta.servlet.Servlet", null)
				&& ClassUtils.isPresent("org.springframework.web.context.WebApplicationContext", null)) {
			ServletTrigger.apply(applicationContext, scheduleHealth);
		}
		GrpcServerTrigger.apply(applicationContext, scheduleHealth);
	}

	/**
	 * Trigger for servlet environments.
	 */
	static class ServletTrigger {

		static void apply(ConfigurableApplicationContext applicationContext, SingletonSupplier<?> scheduleHealth) {
			if (hasGrpcServletRegistration(applicationContext)) {
				scheduleHealth.get();
				return;
			}
		}

		private static boolean hasGrpcServletRegistration(ConfigurableApplicationContext applicationContext) {
			if (!ObjectUtils.isEmpty(applicationContext.getBeanNamesForType(GrpcServletRegistration.class))) {
				return true;
			}
			if (applicationContext instanceof WebApplicationContext webApplicationContext) {
				ServletContext servletContext = webApplicationContext.getServletContext();
				if (servletContext != null && servletContext.getServletRegistrations()
					.values()
					.stream()
					.anyMatch((servletRegistration) -> "io.grpc.servlet.jakarta.GrpcServlet"
						.equals(servletRegistration.getClassName()))) {
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * Trigger for regular gRPC servers.
	 */
	static class GrpcServerTrigger {

		static void apply(ConfigurableApplicationContext applicationContext, SingletonSupplier<?> scheduleHealth) {
			applicationContext.addApplicationListener((event) -> {
				if (event instanceof GrpcServerStartedEvent) {
					scheduleHealth.get();
				}
			});
		}

	}

}
