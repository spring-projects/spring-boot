/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.export.jmx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyJmxConfiguration {

	@Bean
	public JmxMeterRegistry jmxMeterRegistry(JmxConfig config, Clock clock) {
		return new JmxMeterRegistry(config, clock, this::toHierarchicalName);
	}

	private String toHierarchicalName(Meter.Id id, NamingConvention convention) {
		return /**/ HierarchicalNameMapper.DEFAULT.toHierarchicalName(id, convention);
	}

}
