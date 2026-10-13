/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.humio;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.humio.HumioConfig;
import io.micrometer.humio.HumioMeterRegistry;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Humio.
 *
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(HumioMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("humio")
@EnableConfigurationProperties(HumioProperties.class)
public final class HumioMetricsExportAutoConfiguration {

	private final HumioProperties properties;

	HumioMetricsExportAutoConfiguration(HumioProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	HumioConfig humioConfig() {
		return new HumioPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	HumioMeterRegistry humioMeterRegistry(HumioConfig humioConfig, Clock clock) {
		return HumioMeterRegistry.builder(humioConfig)
			.clock(clock)
			.httpClient(
					new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
			.build();

	}

}
