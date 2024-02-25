/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.howto.actuator.maphealthindicatorstometrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Configuration;

/**
 * MyHealthMetricsExportConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyHealthMetricsExportConfiguration {

	/**
     * Constructs a new MyHealthMetricsExportConfiguration with the given MeterRegistry and HealthEndpoint.
     * 
     * @param registry the MeterRegistry to register the health gauge with
     * @param healthEndpoint the HealthEndpoint to retrieve the health status from
     */
    public MyHealthMetricsExportConfiguration(MeterRegistry registry, HealthEndpoint healthEndpoint) {
		// This example presumes common tags (such as the app) are applied elsewhere
		Gauge.builder("health", healthEndpoint, this::getStatusCode).strongReference(true).register(registry);
	}

	/**
     * Returns the status code based on the health status of the provided HealthEndpoint.
     * 
     * @param health the HealthEndpoint to get the status from
     * @return the status code:
     *         - 3 if the status is UP
     *         - 2 if the status is OUT_OF_SERVICE
     *         - 1 if the status is DOWN
     *         - 0 if the status is unknown or null
     */
    private int getStatusCode(HealthEndpoint health) {
		Status status = health.health().getStatus();
		if (Status.UP.equals(status)) {
			return 3;
		}
		if (Status.OUT_OF_SERVICE.equals(status)) {
			return 2;
		}
		if (Status.DOWN.equals(status)) {
			return 1;
		}
		return 0;
	}

}
