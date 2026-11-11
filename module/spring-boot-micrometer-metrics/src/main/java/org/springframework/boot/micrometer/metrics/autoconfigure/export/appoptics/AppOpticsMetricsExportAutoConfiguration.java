/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.appoptics;

import io.micrometer.appoptics.AppOpticsConfig;
import io.micrometer.appoptics.AppOpticsMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.ipc.http.HttpUrlConnectionSender;

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
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to AppOptics.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 * @since 4.0.0
 * @deprecated since 4.2.0 for removal in 4.4.0 with no replacement as AppOptics has
 * retired
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(AppOpticsMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("appoptics")
@EnableConfigurationProperties(AppOpticsProperties.class)
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
public final class AppOpticsMetricsExportAutoConfiguration {

	private final AppOpticsProperties properties;

	AppOpticsMetricsExportAutoConfiguration(AppOpticsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	AppOpticsConfig appOpticsConfig() {
		return new AppOpticsPropertiesConfigAdapter(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	AppOpticsMeterRegistry appOpticsMeterRegistry(AppOpticsConfig config, Clock clock) {
		return AppOpticsMeterRegistry.builder(config)
			.clock(clock)
			.httpClient(
					new HttpUrlConnectionSender(this.properties.getConnectTimeout(), this.properties.getReadTimeout()))
			.build();
	}

}
