/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.export.graphite;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteMeterRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyGraphiteConfiguration {

	@Bean
	public GraphiteMeterRegistry graphiteMeterRegistry(GraphiteConfig config, Clock clock) {
		return new GraphiteMeterRegistry(config, clock, this::toHierarchicalName);
	}

	private String toHierarchicalName(Meter.Id id, NamingConvention convention) {
		return /**/ HierarchicalNameMapper.DEFAULT.toHierarchicalName(id, convention);
	}

}
