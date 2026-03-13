/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp;

import java.time.Duration;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.ExemplarContextProvider;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpHttpMetricsSender;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetryProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to OTLP.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass({ OtlpMeterRegistry.class, OpenTelemetryProperties.class })
@ConditionalOnEnabledMetricsExport("otlp")
@EnableConfigurationProperties({ OtlpMetricsProperties.class, OpenTelemetryProperties.class })
public final class OtlpMetricsExportAutoConfiguration {

	private final OtlpMetricsProperties properties;

	OtlpMetricsExportAutoConfiguration(OtlpMetricsProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	OtlpMetricsConnectionDetails otlpMetricsConnectionDetails(ObjectProvider<SslBundles> sslBundles) {
		return new PropertiesOtlpMetricsConnectionDetails(this.properties, sslBundles.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	OtlpConfig otlpConfig(OpenTelemetryProperties openTelemetryProperties,
			OtlpMetricsConnectionDetails connectionDetails, Environment environment) {
		return new OtlpMetricsPropertiesConfigAdapter(this.properties, openTelemetryProperties, connectionDetails,
				environment);
	}

	@Bean
	@ConditionalOnMissingBean(OtlpMetricsSender.class)
	OtlpHttpMetricsSender otlpMetricsSender(OtlpMetricsConnectionDetails connectionDetails) {
		Duration connectTimeout = this.properties.getConnectTimeout();
		Duration timeout = connectTimeout.plus(this.properties.getReadTimeout());
		JdkClientHttpSender httpSender = new JdkClientHttpSender(connectTimeout, timeout,
				connectionDetails.getSslBundle());
		return new OtlpHttpMetricsSender(httpSender);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	OtlpMeterRegistry otlpMeterRegistry(OtlpConfig otlpConfig, Clock clock, OtlpMetricsSender metricsSender,
			ObjectProvider<ExemplarContextProvider> exemplarContextProvider) {
		return builder(otlpConfig, clock, metricsSender, exemplarContextProvider).build();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	OtlpMeterRegistry otlpMeterRegistryVirtualThreads(OtlpConfig otlpConfig, Clock clock,
			OtlpMetricsSender metricsSender, ObjectProvider<ExemplarContextProvider> exemplarContextProvider) {
		VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor("otlp-meter-registry-");
		return builder(otlpConfig, clock, metricsSender, exemplarContextProvider)
			.threadFactory(executor.getVirtualThreadFactory())
			.build();
	}

	private OtlpMeterRegistry.Builder builder(OtlpConfig otlpConfig, Clock clock, OtlpMetricsSender metricsSender,
			ObjectProvider<ExemplarContextProvider> exemplarContextProvider) {
		OtlpMeterRegistry.Builder builder = OtlpMeterRegistry.builder(otlpConfig)
			.clock(clock)
			.metricsSender(metricsSender);
		exemplarContextProvider.ifAvailable(builder::exemplarContextProvider);
		return builder;
	}

	/**
	 * Adapts {@link OtlpMetricsProperties} to {@link OtlpMetricsConnectionDetails}.
	 */
	static class PropertiesOtlpMetricsConnectionDetails implements OtlpMetricsConnectionDetails {

		private final OtlpMetricsProperties properties;

		private final @Nullable SslBundles sslBundles;

		PropertiesOtlpMetricsConnectionDetails(OtlpMetricsProperties properties, @Nullable SslBundles sslBundles) {
			this.properties = properties;
			this.sslBundles = sslBundles;
		}

		@Override
		public @Nullable String getUrl() {
			return this.properties.getUrl();
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			String bundleName = this.properties.getSsl().getBundle();
			if (StringUtils.hasLength(bundleName)) {
				Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
				return this.sslBundles.getBundle(bundleName);
			}
			return null;
		}

	}

}
