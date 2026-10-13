/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.gettingstarted.specifictype

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.config.NamingConvention
import io.micrometer.graphite.GraphiteMeterRegistry
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyMeterRegistryConfiguration {

	@Bean
	fun graphiteMetricsNamingConvention(): MeterRegistryCustomizer<GraphiteMeterRegistry> {
		return MeterRegistryCustomizer { registry: GraphiteMeterRegistry ->
			registry.config().namingConvention(this::name)
		}
	}

	private fun name(name: String, type: Meter.Type, baseUnit: String?): String {
		return  /**/ NamingConvention.snakeCase.name(name, type, baseUnit)
	}

}

