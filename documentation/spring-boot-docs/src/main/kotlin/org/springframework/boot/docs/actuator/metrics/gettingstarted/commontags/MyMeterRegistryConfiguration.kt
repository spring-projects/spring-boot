/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.gettingstarted.commontags

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyMeterRegistryConfiguration {

	@Bean
	fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
		return MeterRegistryCustomizer { registry ->
			registry.config().commonTags("region", "us-east-1")
		}
	}

}

