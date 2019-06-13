/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JvmMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class JvmMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(JvmMetricsAutoConfiguration.class));

	@Test
	void autoConfiguresJvmMetrics() {
		this.contextRunner.run(
				(context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class).hasSingleBean(JvmMemoryMetrics.class)
						.hasSingleBean(JvmThreadMetrics.class).hasSingleBean(ClassLoaderMetrics.class));
	}

	@Test
	void allowsCustomJvmGcMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmGcMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class).hasBean("customJvmGcMetrics")
						.hasSingleBean(JvmMemoryMetrics.class).hasSingleBean(JvmThreadMetrics.class)
						.hasSingleBean(ClassLoaderMetrics.class));
	}

	@Test
	void allowsCustomJvmMemoryMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmMemoryMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
						.hasSingleBean(JvmMemoryMetrics.class).hasBean("customJvmMemoryMetrics")
						.hasSingleBean(JvmThreadMetrics.class).hasSingleBean(ClassLoaderMetrics.class));
	}

	@Test
	void allowsCustomJvmThreadMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmThreadMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
						.hasSingleBean(JvmMemoryMetrics.class).hasSingleBean(JvmThreadMetrics.class)
						.hasSingleBean(ClassLoaderMetrics.class).hasBean("customJvmThreadMetrics"));
	}

	@Test
	void allowsCustomClassLoaderMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClassLoaderMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
						.hasSingleBean(JvmMemoryMetrics.class).hasSingleBean(JvmThreadMetrics.class)
						.hasSingleBean(ClassLoaderMetrics.class).hasBean("customClassLoaderMetrics"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmGcMetricsConfiguration {

		@Bean
		public JvmGcMetrics customJvmGcMetrics() {
			return new JvmGcMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmMemoryMetricsConfiguration {

		@Bean
		public JvmMemoryMetrics customJvmMemoryMetrics() {
			return new JvmMemoryMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmThreadMetricsConfiguration {

		@Bean
		public JvmThreadMetrics customJvmThreadMetrics() {
			return new JvmThreadMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClassLoaderMetricsConfiguration {

		@Bean
		public ClassLoaderMetrics customClassLoaderMetrics() {
			return new ClassLoaderMetrics();
		}

	}

}
