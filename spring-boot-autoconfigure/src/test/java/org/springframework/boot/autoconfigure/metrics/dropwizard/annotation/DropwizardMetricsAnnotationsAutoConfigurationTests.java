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

package org.springframework.boot.autoconfigure.metrics.dropwizard.annotation;

import java.util.ArrayList;
import java.util.List;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurer;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DropwizardMetricsAnnotationsAutoConfiguration}.
 *
 * @author Sergey Kuptsov
 */
public class DropwizardMetricsAnnotationsAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void canEnableConfiguration() {
		this.context.register(DropwizardMetricsAnnotationsAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(MetricsConfigurer.class)).isNotEmpty();
	}

	@Test
	public void configurationHealthCheckEmptyByDefault() {
		this.context.register(MetricsConfigurerApplicationTestConfiguration.class);
		this.context.register(DropwizardMetricsAnnotationsAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(MetricsConfigurer.class)).hasSize(1);
		MetricsConfigurer metricsConfigurer = this.context.getBean(MetricsConfigurer.class);
		assertThat(metricsConfigurer.getHealthCheckRegistry() == null);
	}

	@Test
	public void configurationHealthCheckEnabled() {
		this.context.register(DropwizardMetricsAnnotationsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.metrics.annotation.healthCheck:true");
		this.context.refresh();

		MetricsConfigurer metricsConfigurer = (MetricsConfigurer) this.context.getBean("metricsConfigurer");
		assertThat(metricsConfigurer.getHealthCheckRegistry() != null);
	}

	@Test
	public void configurationJmxReporterEnabled() {
		this.context.register(TracingMetricRegistryConfiguration.class);
		this.context.register(DropwizardMetricsAnnotationsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.metrics.annotation.reporter.jmx.enabled:true");
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.metrics.annotation.metricsRegistryBeanName:tracingMetricRegistry");
		this.context.refresh();

		MetricsConfigurer metricsConfigurer = (MetricsConfigurer) this.context.getBean("metricsConfigurer");
		MetricRegistry metricRegistry = metricsConfigurer.getMetricRegistry();

		assertThat(metricRegistry instanceof TracingMetricRegistryConfiguration.TracingMetricRegistry);
		@SuppressWarnings("ConstantConditions")
		TracingMetricRegistryConfiguration.TracingMetricRegistry tracingMetricRegistry =
				(TracingMetricRegistryConfiguration.TracingMetricRegistry) metricRegistry;

		List<MetricRegistryListener> listeners = tracingMetricRegistry.listeners;
		assertThat(listeners.size() == 1);
		MetricRegistryListener metricRegistryListener = listeners.get(0);
		assertThat(metricRegistryListener != null);
		assertThat(metricRegistryListener instanceof JmxReporter);
	}

	@Test
	public void configurationJmxReporterNotEnabledByDefault() {
		this.context.register(TracingMetricRegistryConfiguration.class);
		this.context.register(DropwizardMetricsAnnotationsAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.metrics.annotation.metricsRegistryBeanName:tracingMetricRegistry");
		this.context.refresh();

		MetricsConfigurer metricsConfigurer = (MetricsConfigurer) this.context.getBean("metricsConfigurer");
		MetricRegistry metricRegistry = metricsConfigurer.getMetricRegistry();

		assertThat(metricRegistry instanceof TracingMetricRegistryConfiguration.TracingMetricRegistry);
		@SuppressWarnings("ConstantConditions")
		TracingMetricRegistryConfiguration.TracingMetricRegistry tracingMetricRegistry =
				(TracingMetricRegistryConfiguration.TracingMetricRegistry) metricRegistry;

		List<MetricRegistryListener> listeners = tracingMetricRegistry.listeners;
		assertThat(listeners.isEmpty());
	}

	@Test
	public void configurationNotOverridesExisting() {
		this.context.register(MetricsConfigurerApplicationTestConfiguration.class);
		this.context.register(DropwizardMetricsAnnotationsAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(MetricsConfigurer.class)).hasSize(1);
		MetricsConfigurer metricsConfigurer = this.context.getBean(MetricsConfigurer.class);
		assertThat(metricsConfigurer.getHealthCheckRegistry() != null);
	}

	@Configuration
	protected static class MetricsConfigurerApplicationTestConfiguration {

		@Bean
		public MetricsConfigurer metricsConfigurer() {
			return new MetricsConfigurerAdapter() {

				@Override
				public HealthCheckRegistry getHealthCheckRegistry() {
					return new HealthCheckRegistry();
				}
			};
		}
	}

	@Configuration
	protected static class TracingMetricRegistryConfiguration {

		@Bean(name = "tracingMetricRegistry")
		public MetricRegistry metricRegistry() {
			return new TracingMetricRegistry();
		}

		protected static class TracingMetricRegistry extends MetricRegistry {

			public List<MetricRegistryListener> listeners = new ArrayList<>();

			@Override
			public void addListener(MetricRegistryListener listener) {
				this.listeners.add(listener);
			}
		}
	}
}
