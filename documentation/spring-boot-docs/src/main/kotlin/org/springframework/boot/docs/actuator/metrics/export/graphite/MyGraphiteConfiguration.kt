/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.export.graphite

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.config.NamingConvention
import io.micrometer.core.instrument.util.HierarchicalNameMapper
import io.micrometer.graphite.GraphiteConfig
import io.micrometer.graphite.GraphiteMeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyGraphiteConfiguration {

	@Bean
	fun graphiteMeterRegistry(config: GraphiteConfig, clock: Clock): GraphiteMeterRegistry {
		return GraphiteMeterRegistry(config, clock, this::toHierarchicalName)
	}
	private fun toHierarchicalName(id: Meter.Id, convention: NamingConvention): String {
		return  /**/ HierarchicalNameMapper.DEFAULT.toHierarchicalName(id, convention)
	}

}

