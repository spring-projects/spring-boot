/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.influx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Influx.
 *
 * @author Jon Schneider
 * @author Artsiom Yudovin
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(InfluxMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("influx")
@EnableConfigurationProperties(InfluxProperties.class)
public final class InfluxMetricsExportAutoConfiguration {

	private final InfluxProperties properties;

	InfluxMetricsExportAutoConfiguration(InfluxProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	InfluxConfig influxConfig() {
		return new InfluxPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	InfluxMeterRegistry influxMeterRegistry(InfluxConfig influxConfig, Clock clock) {
		return InfluxMeterRegistry.builder(influxConfig)
			.clock(clock)
			.httpClient(
					new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
			.build();

	}

}
