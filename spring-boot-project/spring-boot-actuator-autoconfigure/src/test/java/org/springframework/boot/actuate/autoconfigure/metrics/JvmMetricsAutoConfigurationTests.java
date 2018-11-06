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

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import org.junit.Test;

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
public class JvmMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(JvmMetricsAutoConfiguration.class));

	@Test
	public void autoConfiguresJvmMetrics() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(JvmGcMetrics.class).hasSingleBean(JvmMemoryMetrics.class)
				.hasSingleBean(JvmThreadMetrics.class)
				.hasSingleBean(ClassLoaderMetrics.class));
	}

	@Test
	@Deprecated
	public void allowsJvmMetricsToBeDisabled() {
		this.contextRunner
				.withPropertyValues("management.metrics.binders.jvm.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(JvmGcMetrics.class)
						.doesNotHaveBean(JvmMemoryMetrics.class)
						.doesNotHaveBean(JvmThreadMetrics.class)
						.doesNotHaveBean(ClassLoaderMetrics.class));
	}

	@Test
	public void allowsCustomJvmGcMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmGcMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
						.hasBean("customJvmGcMetrics")
						.hasSingleBean(JvmMemoryMetrics.class)
						.hasSingleBean(JvmThreadMetrics.class)
						.hasSingleBean(ClassLoaderMetrics.class));
	}

	@Test
	public void allowsCustomJvmMemoryMetricsToBeUsed() {
		this.contextRunner
				.withUserConfiguration(CustomJvmMemoryMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
						.hasSingleBean(JvmMemoryMetrics.class)
						.hasBean("customJvmMemoryMetrics")
						.hasSingleBean(JvmThreadMetrics.class)
						.hasSingleBean(ClassLoaderMetrics.class));
	}

	@Test
	public void allowsCustomJvmThreadMetricsToBeUsed() {
		this.contextRunner
				.withUserConfiguration(CustomJvmThreadMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
						.hasSingleBean(JvmMemoryMetrics.class)
						.hasSingleBean(JvmThreadMetrics.class)
						.hasSingleBean(ClassLoaderMetrics.class)
						.hasBean("customJvmThreadMetrics"));
	}

	@Test
	public void allowsCustomClassLoaderMetricsToBeUsed() {
		this.contextRunner
				.withUserConfiguration(CustomClassLoaderMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(JvmGcMetrics.class)
						.hasSingleBean(JvmMemoryMetrics.class)
						.hasSingleBean(JvmThreadMetrics.class)
						.hasSingleBean(ClassLoaderMetrics.class)
						.hasBean("customClassLoaderMetrics"));
	}

	@Configuration
	static class CustomJvmGcMetricsConfiguration {

		@Bean
		public JvmGcMetrics customJvmGcMetrics() {
			return new JvmGcMetrics();
		}

	}

	@Configuration
	static class CustomJvmMemoryMetricsConfiguration {

		@Bean
		public JvmMemoryMetrics customJvmMemoryMetrics() {
			return new JvmMemoryMetrics();
		}

	}

	@Configuration
	static class CustomJvmThreadMetricsConfiguration {

		@Bean
		public JvmThreadMetrics customJvmThreadMetrics() {
			return new JvmThreadMetrics();
		}

	}

	@Configuration
	static class CustomClassLoaderMetricsConfiguration {

		@Bean
		public ClassLoaderMetrics customClassLoaderMetrics() {
			return new ClassLoaderMetrics();
		}

	}

}
