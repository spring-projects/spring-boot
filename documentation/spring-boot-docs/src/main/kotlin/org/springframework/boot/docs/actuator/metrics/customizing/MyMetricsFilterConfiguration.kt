/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.customizing

import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyMetricsFilterConfiguration {

	@Bean
	fun renameRegionTagMeterFilter(): MeterFilter {
		return MeterFilter.renameTag("com.example", "mytag.region", "mytag.area")
	}

}

