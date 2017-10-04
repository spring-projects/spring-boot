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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

import org.springframework.boot.actuate.autoconfigure.metrics.export.MetricsExporter;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for exporting metrics to Prometheus.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass(PrometheusMeterRegistry.class)
@EnableConfigurationProperties(PrometheusProperties.class)
public class PrometheusExportConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PrometheusConfig prometheusConfig(PrometheusProperties prometheusProperties) {
		return new PrometheusPropertiesConfigAdapter(prometheusProperties);
	}

	@Bean
	@ConditionalOnProperty(value = "spring.metrics.prometheus.enabled", matchIfMissing = true)
	public MetricsExporter prometheusExporter(PrometheusConfig prometheusConfig,
			CollectorRegistry collectorRegistry, Clock clock) {
		return () -> new PrometheusMeterRegistry(prometheusConfig, collectorRegistry,
				clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public CollectorRegistry collectorRegistry() {
		return new CollectorRegistry(true);
	}

	@Bean
	@ConditionalOnMissingBean
	public Clock clock() {
		return Clock.SYSTEM;
	}

	@ManagementContextConfiguration
	public static class PrometheusScrapeEndpointConfiguration {

		@Bean
		public PrometheusScrapeEndpoint prometheusEndpoint(
				CollectorRegistry collectorRegistry) {
			return new PrometheusScrapeEndpoint(collectorRegistry);
		}

	}

}
