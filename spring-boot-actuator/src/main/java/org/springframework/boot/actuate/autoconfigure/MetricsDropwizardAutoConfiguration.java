/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import javax.sql.DataSource;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.dropwizard.DropwizardMetricServices;
import org.springframework.boot.actuate.metrics.reader.MetricRegistryMetricReader;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Dropwizard-based metrics.
 *
 * @author Dave Syer
 * @author Tommy Ludwig
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(MetricRegistry.class)
@AutoConfigureBefore(MetricRepositoryAutoConfiguration.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class MetricsDropwizardAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public MetricRegistry metricRegistry() {
		return new MetricRegistry();
	}

	@Bean
	@ConditionalOnMissingBean({ DropwizardMetricServices.class, CounterService.class,
			GaugeService.class })
	public DropwizardMetricServices dropwizardMetricServices(
			MetricRegistry metricRegistry) {
		return new DropwizardMetricServices(metricRegistry);
	}

	@Bean
	public MetricReaderPublicMetrics dropwizardPublicMetrics(
			MetricRegistry metricRegistry) {
		MetricRegistryMetricReader reader = new MetricRegistryMetricReader(
				metricRegistry);
		return new MetricReaderPublicMetrics(reader);
	}

	@Bean
	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnBean(DataSource.class)
	public HikariMetricRegistryConfigurer hikariMetricRegistryConfigurer(
			MetricRegistry metricRegistry) {
		return new HikariMetricRegistryConfigurer(metricRegistry);
	}

	private static final class HikariMetricRegistryConfigurer
			implements BeanPostProcessor {

		private final MetricRegistry metricRegistry;

		private HikariMetricRegistryConfigurer(MetricRegistry metricRegistry) {
			this.metricRegistry = metricRegistry;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof HikariDataSource) {
				customize((HikariDataSource) bean);
			}
			return bean;
		}

		private void customize(HikariDataSource dataSource) {
			if (dataSource.getMetricRegistry() == null) {
				dataSource.setMetricRegistry(this.metricRegistry);
			}
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}
	}

}
