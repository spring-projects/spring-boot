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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.tracer.common.SpanContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusSimpleclientScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Prometheus.
 *
 * @author Jon Schneider
 * @author David J. M. Karlsen
 * @author Jonatan Ivanov
 * @since 2.0.0
 */
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(PrometheusMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("prometheus")
@EnableConfigurationProperties(PrometheusProperties.class)
public class PrometheusMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	PrometheusConfig prometheusConfig(PrometheusProperties prometheusProperties) {
		return new PrometheusPropertiesConfigAdapter(prometheusProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig prometheusConfig,
			PrometheusRegistry prometheusRegistry, Clock clock, ObjectProvider<SpanContext> spanContext) {
		return new PrometheusMeterRegistry(prometheusConfig, prometheusRegistry, clock, spanContext.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	PrometheusRegistry prometheusRegistry() {
		return new PrometheusRegistry();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnAvailableEndpoint(endpoint = PrometheusScrapeEndpoint.class)
	static class PrometheusScrapeEndpointConfiguration {

		@SuppressWarnings("removal")
		@Bean
		@ConditionalOnMissingBean({ PrometheusScrapeEndpoint.class, PrometheusSimpleclientScrapeEndpoint.class })
		PrometheusScrapeEndpoint prometheusEndpoint(PrometheusRegistry prometheusRegistry) {
			return new PrometheusScrapeEndpoint(prometheusRegistry);
		}

	}

}
