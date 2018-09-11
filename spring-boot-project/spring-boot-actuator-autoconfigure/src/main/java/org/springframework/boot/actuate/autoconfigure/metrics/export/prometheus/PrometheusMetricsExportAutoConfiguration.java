/*
 * Copyright 2012-2018 the original author or authors.
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

import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for exporting metrics to Prometheus.
 *
 * @since 2.0.0
 * @author Jon Schneider
 */
@Configuration
@AutoConfigureBefore({ CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(PrometheusMeterRegistry.class)
@ConditionalOnProperty(prefix = "management.metrics.export.prometheus", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PrometheusProperties.class)
public class PrometheusMetricsExportAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PrometheusConfig prometheusConfig(PrometheusProperties prometheusProperties) {
		return new PrometheusPropertiesConfigAdapter(prometheusProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public PrometheusMeterRegistry prometheusMeterRegistry(
			PrometheusConfig prometheusConfig, CollectorRegistry collectorRegistry,
			Clock clock) {
		return new PrometheusMeterRegistry(prometheusConfig, collectorRegistry, clock);
	}

	@Bean
	@ConditionalOnMissingBean
	public CollectorRegistry collectorRegistry() {
		return new CollectorRegistry(true);
	}

	@Configuration
	public static class PrometheusScrapeEndpointConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		@ConditionalOnMissingBean
		public PrometheusScrapeEndpoint prometheusEndpoint(
				CollectorRegistry collectorRegistry) {
			return new PrometheusScrapeEndpoint(collectorRegistry);
		}

	}

	/**
	 * Configuration for <a href="https://github.com/prometheus/pushgateway">Prometheus
	 * Pushgateway</a>.
	 *
	 * @author David J. M. Karlsen
	 */
	@Configuration
	@ConditionalOnClass(PushGateway.class)
	@ConditionalOnProperty(prefix = "management.metrics.export.prometheus.pushgateway", name = "enabled")
	public static class PrometheusPushGatewayConfiguration {

		@Bean
		public PushGatewayHandler pushGatewayHandler(CollectorRegistry collectorRegistry,
				PrometheusProperties prometheusProperties, Environment environment) {
			return new PushGatewayHandler(collectorRegistry, prometheusProperties,
					environment);
		}

		static class PushGatewayHandler {

			private final Logger logger = LoggerFactory
					.getLogger(PrometheusPushGatewayConfiguration.class);

			private final CollectorRegistry collectorRegistry;

			private final PrometheusProperties.PushgatewayProperties pushgatewayProperties;

			private final PushGateway pushGateway;

			private final Environment environment;

			private final ScheduledExecutorService executorService;

			PushGatewayHandler(CollectorRegistry collectorRegistry,
					PrometheusProperties prometheusProperties, Environment environment) {
				this.collectorRegistry = collectorRegistry;
				this.pushgatewayProperties = prometheusProperties.getPushgateway();
				this.pushGateway = new PushGateway(
						this.pushgatewayProperties.getBaseUrl());
				this.environment = environment;
				this.executorService = Executors.newSingleThreadScheduledExecutor((r) -> {
					Thread thread = new Thread(r);
					thread.setDaemon(true);
					thread.setName("micrometer-pushgateway");
					return thread;
				});
				this.executorService.scheduleAtFixedRate(this::push, 0,
						this.pushgatewayProperties.getPushRate().toMillis(),
						TimeUnit.MILLISECONDS);
			}

			void push() {
				try {
					this.pushGateway.pushAdd(this.collectorRegistry, getJobName(),
							this.pushgatewayProperties.getGroupingKeys());
				}
				catch (UnknownHostException ex) {
					this.logger.error("Unable to locate host '"
							+ this.pushgatewayProperties.getBaseUrl()
							+ "'. No longer attempting metrics publication to this host");
					this.executorService.shutdown();
				}
				catch (Throwable throwable) {
					this.logger.error("Unable to push metrics to Prometheus Pushgateway",
							throwable);
				}
			}

			@PreDestroy
			void shutdown() {
				this.executorService.shutdown();
				if (this.pushgatewayProperties.isPushOnShutdown()) {
					push();
				}
				if (this.pushgatewayProperties.isDeleteOnShutdown()) {
					delete();
				}
			}

			private void delete() {
				try {
					this.pushGateway.delete(getJobName(),
							this.pushgatewayProperties.getGroupingKeys());
				}
				catch (Throwable throwable) {
					this.logger.error(
							"Unable to delete metrics from Prometheus Pushgateway",
							throwable);
				}
			}

			private String getJobName() {
				String job = this.pushgatewayProperties.getJob();
				if (job == null) {
					job = this.environment.getProperty("spring.application.name");
				}
				if (job == null) {
					// There's a history of Prometheus spring integration defaulting the
					// getJobName name to "spring" from when
					// Prometheus integration didn't exist in Spring itself.
					job = "spring";
				}
				return job;
			}

		}

	}

}
