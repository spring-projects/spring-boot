/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.actuator.maphealthindicatorstometrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyHealthMetricsExportConfiguration {

	public MyHealthMetricsExportConfiguration(MeterRegistry registry, HealthEndpoint healthEndpoint) {
		// This example presumes common tags (such as the app) are applied elsewhere
		Gauge.builder("health", healthEndpoint, this::getStatusCode).strongReference(true).register(registry);
	}

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
