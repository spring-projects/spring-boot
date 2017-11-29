/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Collection;
import java.util.Collections;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.export.MetricsExporter;
import org.springframework.boot.actuate.autoconfigure.metrics.export.atlas.AtlasExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.datadog.DatadogExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ganglia.GangliaExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.graphite.GraphiteExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.influx.InfluxExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.jmx.JmxExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.statsd.StatsdExportConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.jdbc.DataSourcePoolMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.reactive.server.WebFluxMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.client.RestTemplateMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsConfiguration;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.integration.SpringIntegrationMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass(Timed.class)
@EnableConfigurationProperties(MetricsProperties.class)
@Import({ MeterBindersConfiguration.class, WebMvcMetricsConfiguration.class,
		WebFluxMetricsConfiguration.class, RestTemplateMetricsConfiguration.class,
		DataSourcePoolMetricsConfiguration.class, AtlasExportConfiguration.class,
		DatadogExportConfiguration.class, GangliaExportConfiguration.class,
		GraphiteExportConfiguration.class, InfluxExportConfiguration.class,
		JmxExportConfiguration.class, PrometheusExportConfiguration.class,
		SimpleExportConfiguration.class, StatsdExportConfiguration.class })
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class MetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(MeterRegistry.class)
	public CompositeMeterRegistry compositeMeterRegistry(
			ObjectProvider<Collection<MetricsExporter>> exporters) {
		CompositeMeterRegistry composite = new CompositeMeterRegistry();
		exporters.getIfAvailable(Collections::emptyList).stream()
				.map(MetricsExporter::registry).forEach(composite::add);
		return composite;
	}

	@Bean
	@ConditionalOnBean(MeterRegistry.class)
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public MetricsEndpoint metricsEndpoint(MeterRegistry registry) {
		return new MetricsEndpoint(registry);
	}

	@Configuration
	@ConditionalOnClass(EnableIntegrationManagement.class)
	static class MetricsIntegrationConfiguration {

		@Bean(name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)
		@ConditionalOnMissingBean(value = IntegrationManagementConfigurer.class, name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME, search = SearchStrategy.CURRENT)
		public IntegrationManagementConfigurer integrationManagementConfigurer() {
			IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
			configurer.setDefaultCountsEnabled(true);
			configurer.setDefaultStatsEnabled(true);
			return configurer;
		}

		@Bean
		public SpringIntegrationMetrics springIntegrationMetrics(
				IntegrationManagementConfigurer configurer) {
			return new SpringIntegrationMetrics(configurer);
		}

	}

	@Configuration
	static class MeterRegistryConfigurationSupport {

		MeterRegistryConfigurationSupport(MeterRegistry registry,
				ObjectProvider<Collection<MeterRegistryConfigurer>> configurers,
				MetricsProperties config,
				ObjectProvider<Collection<MeterBinder>> binders) {
			configurers.getIfAvailable(Collections::emptyList)
					.forEach((configurer) -> configurer.configureRegistry(registry));
			binders.getIfAvailable(Collections::emptyList)
					.forEach((binder) -> binder.bindTo(registry));
			if (config.isUseGlobalRegistry()) {
				Metrics.addRegistry(registry);
			}
		}

	}

}
