/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.lifecycle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.core.log.LogMessage;

/**
 * Utility used to {@link #wait() wait} for {@link RunningService services} to be ready.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ServiceReadinessChecks {

	private static final Log logger = LogFactory.getLog(ServiceReadinessChecks.class);

	private static final String DISABLE_LABEL = "org.springframework.boot.readiness-check.disable";

	private static final Duration SLEEP_BETWEEN_READINESS_TRIES = Duration.ofSeconds(1);

	private final Clock clock;

	private final Consumer<Duration> sleep;

	private final DockerComposeProperties.Readiness properties;

	private final TcpConnectServiceReadinessCheck check;

	/**
	 * Constructs a new ServiceReadinessChecks object with the specified
	 * DockerComposeProperties.Readiness properties.
	 * @param properties the DockerComposeProperties.Readiness properties to use for the
	 * checks
	 */
	ServiceReadinessChecks(DockerComposeProperties.Readiness properties) {
		this(properties, Clock.systemUTC(), ServiceReadinessChecks::sleep,
				new TcpConnectServiceReadinessCheck(properties.getTcp()));
	}

	/**
	 * Performs service readiness checks using the provided properties, clock, sleep
	 * function, and TCP connect readiness check.
	 * @param properties the Docker Compose readiness properties
	 * @param clock the clock used for timing
	 * @param sleep the function used for sleeping
	 * @param check the TCP connect readiness check
	 */
	ServiceReadinessChecks(DockerComposeProperties.Readiness properties, Clock clock, Consumer<Duration> sleep,
			TcpConnectServiceReadinessCheck check) {
		this.clock = clock;
		this.sleep = sleep;
		this.properties = properties;
		this.check = check;
	}

	/**
	 * Wait for the given services to be ready.
	 * @param runningServices the services to wait for
	 */
	void waitUntilReady(List<RunningService> runningServices) {
		Duration timeout = this.properties.getTimeout();
		Instant start = this.clock.instant();
		while (true) {
			List<ServiceNotReadyException> exceptions = check(runningServices);
			if (exceptions.isEmpty()) {
				return;
			}
			Duration elapsed = Duration.between(start, this.clock.instant());
			if (elapsed.compareTo(timeout) > 0) {
				throw new ReadinessTimeoutException(timeout, exceptions);
			}
			this.sleep.accept(SLEEP_BETWEEN_READINESS_TRIES);
		}
	}

	/**
	 * Checks the readiness of a list of running services.
	 * @param runningServices the list of running services to check
	 * @return a list of ServiceNotReadyException if any service is not ready, otherwise
	 * an empty list
	 */
	private List<ServiceNotReadyException> check(List<RunningService> runningServices) {
		List<ServiceNotReadyException> exceptions = null;
		for (RunningService service : runningServices) {
			if (isDisabled(service)) {
				continue;
			}
			logger.trace(LogMessage.format("Checking readiness of service '%s'", service));
			try {
				this.check.check(service);
				logger.trace(LogMessage.format("Service '%s' is ready", service));
			}
			catch (ServiceNotReadyException ex) {
				logger.trace(LogMessage.format("Service '%s' is not ready", service), ex);
				exceptions = (exceptions != null) ? exceptions : new ArrayList<>();
				exceptions.add(ex);
			}
		}
		return (exceptions != null) ? exceptions : Collections.emptyList();
	}

	/**
	 * Checks if a running service is disabled.
	 * @param service the running service to check
	 * @return true if the service is disabled, false otherwise
	 */
	private boolean isDisabled(RunningService service) {
		return service.labels().containsKey(DISABLE_LABEL);
	}

	/**
	 * Suspends the execution of the current thread for the specified duration.
	 * @param duration the duration to sleep for
	 * @throws IllegalArgumentException if the duration is negative
	 * @throws InterruptedException if the current thread is interrupted while sleeping
	 */
	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
