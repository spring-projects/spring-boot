/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.export.jmx

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.config.NamingConvention
import io.micrometer.core.instrument.util.HierarchicalNameMapper
import io.micrometer.jmx.JmxConfig
import io.micrometer.jmx.JmxMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyJmxConfiguration {

	@Bean
	fun jmxMeterRegistry(config: JmxConfig, clock: Clock): JmxMeterRegistry {
		return JmxMeterRegistry(config, clock, this::toHierarchicalName)
	}

	private fun toHierarchicalName(id: Meter.Id, convention: NamingConvention): String {
		return  /**/ HierarchicalNameMapper.DEFAULT.toHierarchicalName(id, convention)
	}

}

