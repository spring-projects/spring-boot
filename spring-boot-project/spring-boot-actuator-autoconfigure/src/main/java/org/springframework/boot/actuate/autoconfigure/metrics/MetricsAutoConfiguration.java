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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.Collection;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.amqp.RabbitMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.cache.CacheMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.jdbc.DataSourcePoolMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.reactive.server.WebFluxMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.client.RestTemplateMetricsConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsConfiguration;
import org.springframework.boot.actuate.metrics.integration.SpringIntegrationMetrics;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(Timed.class)
@EnableConfigurationProperties(MetricsProperties.class)
@Import({ MeterBindersConfiguration.class, WebMvcMetricsConfiguration.class,
		WebFluxMetricsConfiguration.class, RestTemplateMetricsConfiguration.class,
		CacheMetricsConfiguration.class, DataSourcePoolMetricsConfiguration.class,
		RabbitMetricsConfiguration.class })
@AutoConfigureAfter({ CacheAutoConfiguration.class, DataSourceAutoConfiguration.class,
		RabbitAutoConfiguration.class, RestTemplateAutoConfiguration.class })
public class MetricsAutoConfiguration {

	@Bean
	public static CompositeMeterRegistryPostProcessor compositeMeterRegistryPostProcessor() {
		return new CompositeMeterRegistryPostProcessor();
	}

	@Bean
	public static MeterRegistryPostProcessor meterRegistryPostProcessor(
			ObjectProvider<Collection<MeterBinder>> binders,
			ObjectProvider<Collection<MeterFilter>> filters,
			ObjectProvider<Collection<MeterRegistryCustomizer<?>>> customizers,
			MetricsProperties properties) {
		return new MeterRegistryPostProcessor(binders.getIfAvailable(),
				filters.getIfAvailable(), customizers.getIfAvailable(),
				properties.isUseGlobalRegistry());
	}

	@Bean
	@Order(0)
	public PropertiesMeterFilter propertiesMeterFilter(MetricsProperties properties) {
		return new PropertiesMeterFilter(properties);
	}

	/**
	 * Binds metrics from Spring Integration.
	 */
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

}
