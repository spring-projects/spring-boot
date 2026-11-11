/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.customizing;

import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyMetricsFilterConfiguration {

	@Bean
	public MeterFilter renameRegionTagMeterFilter() {
		return MeterFilter.renameTag("com.example", "mytag.region", "mytag.area");
	}

}
