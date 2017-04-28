/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.metrics.dropwizard.annotation;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurer;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Dropwizard application metrics library
 * {@link MetricsConfigurerAdapter}.
 * Configuration allows to use dropwizard http://metrics.ryantenney.com/annotations.
 * Configuration activates only if one metrics reporter like {@link JmxReporter} configured.
 *
 * @author Sergey Kuptsov
 */
@Configuration
@ConditionalOnClass(MetricsConfigurer.class)
@ConditionalOnMissingBean(MetricsConfigurer.class)
@EnableConfigurationProperties(DropwizardMetricsAnnotationsProperties.class)
public class DropwizardMetricsAnnotationsAutoConfiguration {

	private final DropwizardMetricsAnnotationsProperties properties;
	private final ListableBeanFactory beanFactory;

	public DropwizardMetricsAnnotationsAutoConfiguration(
			DropwizardMetricsAnnotationsProperties properties,
			ListableBeanFactory beanFactory) {
		this.properties = properties;
		this.beanFactory = beanFactory;
	}

	/**
	 * Defines bean callback methods to customize the Java-based configuration
	 * for Spring Metrics enabled via {@link EnableMetrics @EnableMetrics}.
	 *
	 * @return configured MetricsConfigurer.
	 */
	@Bean
	public MetricsConfigurer metricsConfigurer() {
		HealthCheckRegistry healthCheckRegistry = this.properties.isHealthCheck() ? new HealthCheckRegistry() : null;
		String metricsRegistryBeanName = this.properties.getMetricsRegistryBeanName();
		MetricRegistry metricRegistry = !StringUtils.isEmpty(metricsRegistryBeanName) ?
				this.beanFactory.containsBean(metricsRegistryBeanName) ?
						this.beanFactory.getBean(metricsRegistryBeanName, MetricRegistry.class)
						: null
				: null;

		return new MetricsConfigurerAdapter() {
			@Override
			public MetricRegistry getMetricRegistry() {
				return metricRegistry;
			}

			@Override
			public HealthCheckRegistry getHealthCheckRegistry() {
				return healthCheckRegistry;
			}

			@Override
			public void configureReporters(MetricRegistry metricRegistry) {
				DropwizardMetricsAnnotationsProperties.Reporter reporter = DropwizardMetricsAnnotationsAutoConfiguration.this.properties.getReporter();

				if (reporter.getJmx().isEnabled()) {
					registerReporter(JmxReporter
							.forRegistry(metricRegistry)
							.build())
							.start();
				}

				if (reporter.getConsole().isEnabled()) {
					registerReporter(ConsoleReporter
							.forRegistry(metricRegistry)
							.build())
							.start(reporter.getConsole().getPeriodSec(),
									reporter.getConsole().getTimeUnit());
				}

				if (reporter.getCsv().isEnabled()) {
					registerReporter(CsvReporter
							.forRegistry(metricRegistry)
							.build(reporter.getCsv().getFile()))
							.start(reporter.getConsole().getPeriodSec(),
									reporter.getConsole().getTimeUnit());
				}

				if (reporter.getSlf4j().isEnabled()) {
					registerReporter(Slf4jReporter
							.forRegistry(metricRegistry)
							.build())
							.start(reporter.getConsole().getPeriodSec(),
									reporter.getConsole().getTimeUnit());
				}
			}
		};
	}

	@Configuration
	@EnableMetrics(proxyTargetClass = true)
	public static class EnableMetricsConfiguration {
	}
}
