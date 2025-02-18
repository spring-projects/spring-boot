/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.otlp;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to OTLP.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(OtlpMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("otlp")
@EnableConfigurationProperties({ OtlpMetricsProperties.class, OpenTelemetryProperties.class })
public class OtlpMetricsExportAutoConfiguration {

	private final OtlpMetricsProperties properties;

	OtlpMetricsExportAutoConfiguration(OtlpMetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	OtlpMetricsConnectionDetails otlpMetricsConnectionDetails() {
		return new PropertiesOtlpMetricsConnectionDetails(this.properties);
	}

	@Bean
	@ConditionalOnMissingBean
	OtlpConfig otlpConfig(OpenTelemetryProperties openTelemetryProperties,
			OtlpMetricsConnectionDetails connectionDetails, Environment environment) {
		return new OtlpMetricsPropertiesConfigAdapter(this.properties, openTelemetryProperties, connectionDetails,
				environment);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	public OtlpMeterRegistry otlpMeterRegistry(OtlpConfig otlpConfig, Clock clock) {
		return new OtlpMeterRegistry(otlpConfig, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	public OtlpMeterRegistry otlpMeterRegistryVirtualThreads(OtlpConfig otlpConfig, Clock clock) {
		VirtualThreadTaskExecutor taskExecutor = new VirtualThreadTaskExecutor("otlp-meter-registry-");
		return new OtlpMeterRegistry(otlpConfig, clock, taskExecutor.getVirtualThreadFactory());
	}

	/**
	 * Adapts {@link OtlpMetricsProperties} to {@link OtlpMetricsConnectionDetails}.
	 */
	static class PropertiesOtlpMetricsConnectionDetails implements OtlpMetricsConnectionDetails {

		private final OtlpMetricsProperties properties;

		PropertiesOtlpMetricsConnectionDetails(OtlpMetricsProperties properties) {
			this.properties = properties;
		}

		@Override
		public String getUrl() {
			return this.properties.getUrl();
		}

	}

}
