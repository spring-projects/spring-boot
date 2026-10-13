/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.kairos;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;
import io.micrometer.kairos.KairosConfig;
import io.micrometer.kairos.KairosMeterRegistry;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to KairosDB.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(KairosMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("kairos")
@EnableConfigurationProperties(KairosProperties.class)
public final class KairosMetricsExportAutoConfiguration {

	private final KairosProperties properties;

	KairosMetricsExportAutoConfiguration(KairosProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	KairosConfig kairosConfig() {
		return new KairosPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	KairosMeterRegistry kairosMeterRegistry(KairosConfig kairosConfig, Clock clock) {
		return KairosMeterRegistry.builder(kairosConfig)
			.clock(clock)
			.httpClient(
					new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
			.build();

	}

}
