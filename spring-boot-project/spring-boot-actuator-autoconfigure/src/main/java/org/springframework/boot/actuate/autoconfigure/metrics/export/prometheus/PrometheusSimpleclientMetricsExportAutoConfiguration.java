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

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exemplars.DefaultExemplarSampler;
import io.prometheus.client.exemplars.ExemplarSampler;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import io.prometheus.client.exporter.BasicAuthHttpConnectionFactory;
import io.prometheus.client.exporter.PushGateway;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.ConditionalOnEnabledMetricsExport;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager.ShutdownOperation;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusSimpleclientScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Prometheus
 * with the Prometheus simpleclient.
 *
 * @author Jon Schneider
 * @author David J. M. Karlsen
 * @author Jonatan Ivanov
 * @since 2.0.0
 * @deprecated since 3.3.0 for removal in 3.5.0 in favor of
 * {@link PrometheusMetricsExportAutoConfiguration}
 */
@Deprecated(since = "3.3.0", forRemoval = true)
@AutoConfiguration(
		before = { CompositeMeterRegistryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class },
		after = { MetricsAutoConfiguration.class, PrometheusMetricsExportAutoConfiguration.class })
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(PrometheusMeterRegistry.class)
@ConditionalOnEnabledMetricsExport("prometheus")
@EnableConfigurationProperties(PrometheusProperties.class)
public class PrometheusSimpleclientMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	PrometheusConfig simpleclientPrometheusConfig(PrometheusProperties prometheusProperties) {
		return new PrometheusSimpleclientPropertiesConfigAdapter(prometheusProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	io.micrometer.prometheus.PrometheusMeterRegistry simpleclientPrometheusMeterRegistry(
			io.micrometer.prometheus.PrometheusConfig prometheusConfig, CollectorRegistry collectorRegistry,
			Clock clock, ObjectProvider<ExemplarSampler> exemplarSamplerProvider) {
		return new io.micrometer.prometheus.PrometheusMeterRegistry(prometheusConfig, collectorRegistry, clock,
				exemplarSamplerProvider.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	CollectorRegistry collectorRegistry() {
		return new CollectorRegistry(true);
	}

	@Bean
	@ConditionalOnMissingBean(ExemplarSampler.class)
	@ConditionalOnBean(SpanContextSupplier.class)
	DefaultExemplarSampler exemplarSampler(SpanContextSupplier spanContextSupplier) {
		return new DefaultExemplarSampler(spanContextSupplier);
	}

	@SuppressWarnings("removal")
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnAvailableEndpoint(endpoint = PrometheusSimpleclientScrapeEndpoint.class)
	static class PrometheusScrapeEndpointConfiguration {

		@Bean
		@ConditionalOnMissingBean({ PrometheusSimpleclientScrapeEndpoint.class, PrometheusScrapeEndpoint.class })
		PrometheusSimpleclientScrapeEndpoint prometheusEndpoint(CollectorRegistry collectorRegistry) {
			return new PrometheusSimpleclientScrapeEndpoint(collectorRegistry);
		}

	}

	/**
	 * Configuration for <a href="https://github.com/prometheus/pushgateway">Prometheus
	 * Pushgateway</a>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(PushGateway.class)
	@ConditionalOnProperty(prefix = "management.prometheus.metrics.export.pushgateway", name = "enabled")
	static class PrometheusPushGatewayConfiguration {

		/**
		 * The fallback job name. We use 'spring' since there's a history of Prometheus
		 * spring integration defaulting to that name from when Prometheus integration
		 * didn't exist in Spring itself.
		 */
		private static final String FALLBACK_JOB = "spring";

		@Bean
		@ConditionalOnMissingBean
		PrometheusPushGatewayManager prometheusPushGatewayManager(CollectorRegistry collectorRegistry,
				PrometheusProperties prometheusProperties, Environment environment) throws MalformedURLException {
			PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
			Duration pushRate = properties.getPushRate();
			String job = getJob(properties, environment);
			Map<String, String> groupingKey = properties.getGroupingKey();
			ShutdownOperation shutdownOperation = properties.getShutdownOperation();
			PushGateway pushGateway = initializePushGateway(properties.getBaseUrl());
			if (StringUtils.hasText(properties.getUsername())) {
				pushGateway.setConnectionFactory(
						new BasicAuthHttpConnectionFactory(properties.getUsername(), properties.getPassword()));
			}
			return new PrometheusPushGatewayManager(pushGateway, collectorRegistry, pushRate, job, groupingKey,
					shutdownOperation);
		}

		private PushGateway initializePushGateway(String url) throws MalformedURLException {
			return new PushGateway(new URL(url));
		}

		private String getJob(PrometheusProperties.Pushgateway properties, Environment environment) {
			String job = properties.getJob();
			job = (job != null) ? job : environment.getProperty("spring.application.name");
			return (job != null) ? job : FALLBACK_JOB;
		}

	}

}
