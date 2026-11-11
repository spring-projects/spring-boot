/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.actuator.maphealthindicatorstometrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.health.contributor.Status
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyHealthMetricsExportConfiguration(registry: MeterRegistry, healthEndpoint: HealthEndpoint) {

	init {
		// This example presumes common tags (such as the app) are applied elsewhere
		Gauge.builder("health", healthEndpoint) { health ->
			getStatusCode(health).toDouble()
		}.strongReference(true).register(registry)
	}

	private fun getStatusCode(health: HealthEndpoint) = when (health.health().status) {
		Status.UP -> 3
		Status.OUT_OF_SERVICE -> 2
		Status.DOWN -> 1
		else -> 0
	}

}

