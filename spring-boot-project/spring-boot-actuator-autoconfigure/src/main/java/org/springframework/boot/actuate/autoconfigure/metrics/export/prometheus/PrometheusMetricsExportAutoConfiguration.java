/*
 * Copyright 2012-2025 the original author or authors.
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
import io.prometheus.metrics.exporter.pushgateway.Format;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import io.prometheus.metrics.exporter.pushgateway.PushGateway.Builder;
import io.prometheus.metrics.exporter.pushgateway.Scheme;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.tracer.common.SpanContext;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

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
	@ConditionalOnAvailableEndpoint(PrometheusScrapeEndpoint.class)
	static class PrometheusScrapeEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean
		PrometheusScrapeEndpoint prometheusEndpoint(PrometheusRegistry prometheusRegistry,
				PrometheusConfig prometheusConfig) {
			return new PrometheusScrapeEndpoint(prometheusRegistry, prometheusConfig.prometheusProperties());
		}

	}

	/**
	 * Configuration for <a href="https://github.com/prometheus/pushgateway">Prometheus
	 * Pushgateway</a>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(PushGateway.class)
	@ConditionalOnBooleanProperty("management.prometheus.metrics.export.pushgateway.enabled")
	static class PrometheusPushGatewayConfiguration {

		/**
		 * The fallback job name. We use 'spring' since there's a history of Prometheus
		 * spring integration defaulting to that name from when Prometheus integration
		 * didn't exist in Spring itself.
		 */
		private static final String FALLBACK_JOB = "spring";

		@Bean
		@ConditionalOnMissingBean
		PrometheusPushGatewayManager prometheusPushGatewayManager(PrometheusRegistry registry,
				PrometheusProperties prometheusProperties, Environment environment) {
			PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
			PushGateway pushGateway = initializePushGateway(registry, properties, environment);
			return new PrometheusPushGatewayManager(pushGateway, properties.getPushRate(),
					properties.getShutdownOperation());
		}

		private PushGateway initializePushGateway(PrometheusRegistry registry,
				PrometheusProperties.Pushgateway properties, Environment environment) {
			Builder builder = PushGateway.builder()
				.address(properties.getAddress())
				.scheme(scheme(properties))
				.format(format(properties))
				.job(getJob(properties, environment))
				.registry(registry);
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("management.prometheus.metrics.export.pushgateway.token", properties.getToken());
				entries.put("management.prometheus.metrics.export.pushgateway.username", properties.getUsername());
			});
			if (StringUtils.hasText(properties.getToken())) {
				builder.bearerToken(properties.getToken());
			}
			else if (StringUtils.hasText(properties.getUsername())) {
				builder.basicAuth(properties.getUsername(), properties.getPassword());
			}
			properties.getGroupingKey().forEach(builder::groupingKey);
			return builder.build();
		}

		private Scheme scheme(PrometheusProperties.Pushgateway properties) {
			return switch (properties.getScheme()) {
				case HTTP -> Scheme.HTTP;
				case HTTPS -> Scheme.HTTPS;
			};
		}

		private Format format(PrometheusProperties.Pushgateway properties) {
			return switch (properties.getFormat()) {
				case PROTOBUF -> Format.PROMETHEUS_PROTOBUF;
				case TEXT -> Format.PROMETHEUS_TEXT;
			};
		}

		private String getJob(PrometheusProperties.Pushgateway properties, Environment environment) {
			String job = properties.getJob();
			job = (job != null) ? job : environment.getProperty("spring.application.name");
			return (job != null) ? job : FALLBACK_JOB;
		}

	}

}
