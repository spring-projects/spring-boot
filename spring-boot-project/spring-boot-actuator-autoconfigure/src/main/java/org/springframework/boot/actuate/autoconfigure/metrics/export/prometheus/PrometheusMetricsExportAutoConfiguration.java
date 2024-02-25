/*
 * Copyright 2012-2023 the original author or authors.
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

	/**
     * Creates a PrometheusConfig bean if no other bean of the same type is present.
     * 
     * @param prometheusProperties the PrometheusProperties object used to configure the PrometheusConfig bean
     * @return the PrometheusConfig bean
     */
    @Bean
	@ConditionalOnMissingBean
	public PrometheusConfig prometheusConfig(PrometheusProperties prometheusProperties) {
		return new PrometheusPropertiesConfigAdapter(prometheusProperties);
	}

	/**
     * Creates a PrometheusMeterRegistry bean if there is no existing bean of the same type.
     * 
     * @param prometheusConfig The PrometheusConfig object used for configuration.
     * @param collectorRegistry The CollectorRegistry object used for collecting metrics.
     * @param clock The Clock object used for measuring time.
     * @param exemplarSamplerProvider The ObjectProvider for ExemplarSampler, used for sampling exemplars.
     * @return The PrometheusMeterRegistry bean.
     */
    @Bean
	@ConditionalOnMissingBean
	public PrometheusMeterRegistry prometheusMeterRegistry(PrometheusConfig prometheusConfig,
			CollectorRegistry collectorRegistry, Clock clock, ObjectProvider<ExemplarSampler> exemplarSamplerProvider) {
		return new PrometheusMeterRegistry(prometheusConfig, collectorRegistry, clock,
				exemplarSamplerProvider.getIfAvailable());
	}

	/**
     * Creates a new instance of CollectorRegistry if no other bean of type CollectorRegistry is present.
     * 
     * @return the created CollectorRegistry instance
     */
    @Bean
	@ConditionalOnMissingBean
	public CollectorRegistry collectorRegistry() {
		return new CollectorRegistry(true);
	}

	/**
     * Creates a new instance of DefaultExemplarSampler if no other bean of type ExemplarSampler is present and a bean of type SpanContextSupplier is present.
     * 
     * @param spanContextSupplier the bean of type SpanContextSupplier used to create the DefaultExemplarSampler instance
     * @return a new instance of DefaultExemplarSampler
     */
    @Bean
	@ConditionalOnMissingBean(ExemplarSampler.class)
	@ConditionalOnBean(SpanContextSupplier.class)
	public DefaultExemplarSampler exemplarSampler(SpanContextSupplier spanContextSupplier) {
		return new DefaultExemplarSampler(spanContextSupplier);
	}

	/**
     * PrometheusScrapeEndpointConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnAvailableEndpoint(endpoint = PrometheusScrapeEndpoint.class)
	public static class PrometheusScrapeEndpointConfiguration {

		/**
         * Creates a PrometheusScrapeEndpoint bean if no other bean of the same type is present.
         * 
         * @param collectorRegistry the CollectorRegistry to be used by the PrometheusScrapeEndpoint
         * @return the PrometheusScrapeEndpoint bean
         */
        @Bean
		@ConditionalOnMissingBean
		public PrometheusScrapeEndpoint prometheusEndpoint(CollectorRegistry collectorRegistry) {
			return new PrometheusScrapeEndpoint(collectorRegistry);
		}

	}

	/**
	 * Configuration for <a href="https://github.com/prometheus/pushgateway">Prometheus
	 * Pushgateway</a>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(PushGateway.class)
	@ConditionalOnProperty(prefix = "management.prometheus.metrics.export.pushgateway", name = "enabled")
	public static class PrometheusPushGatewayConfiguration {

		/**
		 * The fallback job name. We use 'spring' since there's a history of Prometheus
		 * spring integration defaulting to that name from when Prometheus integration
		 * didn't exist in Spring itself.
		 */
		private static final String FALLBACK_JOB = "spring";

		/**
         * Creates a PrometheusPushGatewayManager bean if no other bean of the same type is present.
         * This bean is responsible for managing the push gateway for Prometheus metrics.
         * 
         * @param collectorRegistry The CollectorRegistry used to register metrics.
         * @param prometheusProperties The PrometheusProperties containing the push gateway configuration.
         * @param environment The Environment object containing the application's environment details.
         * @return The PrometheusPushGatewayManager bean.
         * @throws MalformedURLException If the push gateway URL is malformed.
         */
        @Bean
		@ConditionalOnMissingBean
		public PrometheusPushGatewayManager prometheusPushGatewayManager(CollectorRegistry collectorRegistry,
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

		/**
         * Initializes a PushGateway with the specified URL.
         * 
         * @param url the URL of the PushGateway
         * @return the initialized PushGateway
         * @throws MalformedURLException if the URL is malformed
         */
        private PushGateway initializePushGateway(String url) throws MalformedURLException {
			return new PushGateway(new URL(url));
		}

		/**
         * Retrieves the job name from the PrometheusProperties.Pushgateway object or the Spring environment.
         * If the job name is not found in either, a fallback job name is returned.
         *
         * @param properties   the PrometheusProperties.Pushgateway object containing the job name
         * @param environment  the Spring environment object
         * @return the job name if found, otherwise the fallback job name
         */
        private String getJob(PrometheusProperties.Pushgateway properties, Environment environment) {
			String job = properties.getJob();
			job = (job != null) ? job : environment.getProperty("spring.application.name");
			return (job != null) ? job : FALLBACK_JOB;
		}

	}

}
