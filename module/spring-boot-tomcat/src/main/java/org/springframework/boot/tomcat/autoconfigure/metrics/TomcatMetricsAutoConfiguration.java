/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import org.apache.catalina.Manager;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.tomcat.metrics.TomcatMetricsBinder;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TomcatMetrics}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnWebApplication
@ConditionalOnClass({ TomcatMetrics.class, Manager.class, MeterRegistry.class })
public final class TomcatMetricsAutoConfiguration {

	@Bean
	@ConditionalOnBean(MeterRegistry.class)
	@ConditionalOnMissingBean({ TomcatMetrics.class, TomcatMetricsBinder.class })
	TomcatMetricsBinder tomcatMetricsBinder(MeterRegistry meterRegistry) {
		return new TomcatMetricsBinder(meterRegistry);
	}

}
