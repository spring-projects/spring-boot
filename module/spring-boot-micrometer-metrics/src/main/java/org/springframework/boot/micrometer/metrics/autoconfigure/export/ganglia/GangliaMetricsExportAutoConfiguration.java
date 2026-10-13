/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.ganglia;

import io.micrometer.core.instrument.Clock;
import io.micrometer.ganglia.GangliaConfig;
import io.micrometer.ganglia.GangliaMeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Ganglia.
 *
 * @author Jon Schneider
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(GangliaMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("ganglia")
@EnableConfigurationProperties(GangliaProperties.class)
public final class GangliaMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	GangliaConfig gangliaConfig(GangliaProperties gangliaProperties) {
		return new GangliaPropertiesConfigAdapter(gangliaProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	GangliaMeterRegistry gangliaMeterRegistry(GangliaConfig gangliaConfig, Clock clock) {
		return new GangliaMeterRegistry(gangliaConfig, clock);
	}

}
