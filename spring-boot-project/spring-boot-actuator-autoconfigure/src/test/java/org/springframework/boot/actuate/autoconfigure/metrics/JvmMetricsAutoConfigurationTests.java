/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.micrometer.binder.jvm.ClassLoaderMetrics;
import io.micrometer.binder.jvm.JvmGcMetrics;
import io.micrometer.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.binder.jvm.JvmMemoryMetrics;
import io.micrometer.binder.jvm.JvmThreadMetrics;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
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
		this.contextRunner.run(assertMetricsBeans());
	}

	@Test
	void allowsCustomJvmGcMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmGcMetricsConfiguration.class)
				.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmGcMetrics")));
	}

	@Test
	@Deprecated
	void allowsCustomJvmGcMetricsToBeUsedBackwardsCompatible() {
		this.contextRunner.withUserConfiguration(CustomJvmGcMetricsBackwardsCompatibleConfiguration.class)
				.run(assertMetricsBeans(JvmGcMetrics.class).andThen((context) -> {
					assertThat(context).hasBean("customJvmGcMetrics");
					assertThat(context).doesNotHaveBean(JvmGcMetrics.class);
					assertThat(context).hasSingleBean(io.micrometer.core.instrument.binder.jvm.JvmGcMetrics.class);
				}));
	}

	@Test
	void allowsCustomJvmHeapPressureMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmHeapPressureMetricsConfiguration.class).run(
				assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmHeapPressureMetrics")));
	}

	@Test
	@Deprecated
	void allowsCustomJvmHeapPressureMetricsToBeUsedBackwardsCompatible() {
		this.contextRunner.withUserConfiguration(CustomJvmHeapPressureMetricsBackwardsCompatibleConfiguration.class)
				.run(assertMetricsBeans(JvmHeapPressureMetrics.class).andThen((context) -> {
					assertThat(context).hasBean("customJvmHeapPressureMetrics");
					assertThat(context).doesNotHaveBean(JvmHeapPressureMetrics.class);
					assertThat(context)
							.hasSingleBean(io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics.class);
				}));
	}

	@Test
	void allowsCustomJvmMemoryMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmMemoryMetricsConfiguration.class)
				.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmMemoryMetrics")));
	}

	@Test
	@Deprecated
	void allowsCustomJvmMemoryMetricsToBeUsedBackwardsCompatible() {
		this.contextRunner.withUserConfiguration(CustomJvmMemoryMetricsBackwardsCompatibleConfiguration.class)
				.run(assertMetricsBeans(JvmMemoryMetrics.class).andThen((context) -> {
					assertThat(context).hasBean("customJvmMemoryMetrics");
					assertThat(context).doesNotHaveBean(JvmMemoryMetrics.class);
					assertThat(context).hasSingleBean(io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics.class);
				}));
	}

	@Test
	void allowsCustomJvmThreadMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomJvmThreadMetricsConfiguration.class)
				.run(assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customJvmThreadMetrics")));
	}

	@Test
	@Deprecated
	void allowsCustomJvmThreadMetricsToBeUsedBackwardsCompatible() {
		this.contextRunner.withUserConfiguration(CustomJvmThreadMetricsBackwardsCompatibleConfiguration.class)
				.run(assertMetricsBeans(JvmThreadMetrics.class).andThen((context) -> {
					assertThat(context).hasBean("customJvmThreadMetrics");
					assertThat(context).doesNotHaveBean(JvmThreadMetrics.class);
					assertThat(context).hasSingleBean(io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics.class);
				}));
	}

	@Test
	void allowsCustomClassLoaderMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClassLoaderMetricsConfiguration.class).run(
				assertMetricsBeans().andThen((context) -> assertThat(context).hasBean("customClassLoaderMetrics")));
	}

	@Test
	@Deprecated
	void allowsCustomClassLoaderMetricsToBeUsedBackwardsCompatible() {
		this.contextRunner.withUserConfiguration(CustomClassLoaderMetricsBackwardsCompatibleConfiguration.class)
				.run(assertMetricsBeans(ClassLoaderMetrics.class).andThen((context) -> {
					assertThat(context).hasBean("customClassLoaderMetrics");
					assertThat(context).doesNotHaveBean(ClassLoaderMetrics.class);
					assertThat(context)
							.hasSingleBean(io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics.class);
				}));
	}

	private ContextConsumer<AssertableApplicationContext> assertMetricsBeans(Class<?>... excludes) {
		Set<Class<?>> beans = new HashSet<>(Arrays.asList(JvmGcMetrics.class, JvmHeapPressureMetrics.class,
				JvmMemoryMetrics.class, JvmThreadMetrics.class, ClassLoaderMetrics.class));
		for (Class<?> exclude : excludes) {
			beans.remove(exclude);
		}
		return (context) -> {
			for (Class<?> bean : beans) {
				assertThat(context).hasSingleBean(bean);
			}
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmGcMetricsConfiguration {

		@Bean
		JvmGcMetrics customJvmGcMetrics() {
			return new JvmGcMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmGcMetricsBackwardsCompatibleConfiguration {

		@Bean
		io.micrometer.core.instrument.binder.jvm.JvmGcMetrics customJvmGcMetrics() {
			return new io.micrometer.core.instrument.binder.jvm.JvmGcMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmHeapPressureMetricsConfiguration {

		@Bean
		JvmHeapPressureMetrics customJvmHeapPressureMetrics() {
			return new JvmHeapPressureMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmHeapPressureMetricsBackwardsCompatibleConfiguration {

		@Bean
		io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics customJvmHeapPressureMetrics() {
			return new io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmMemoryMetricsConfiguration {

		@Bean
		JvmMemoryMetrics customJvmMemoryMetrics() {
			return new JvmMemoryMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmMemoryMetricsBackwardsCompatibleConfiguration {

		@Bean
		io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics customJvmMemoryMetrics() {
			return new io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmThreadMetricsConfiguration {

		@Bean
		JvmThreadMetrics customJvmThreadMetrics() {
			return new JvmThreadMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmThreadMetricsBackwardsCompatibleConfiguration {

		@Bean
		io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics customJvmThreadMetrics() {
			return new io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClassLoaderMetricsConfiguration {

		@Bean
		ClassLoaderMetrics customClassLoaderMetrics() {
			return new ClassLoaderMetrics();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClassLoaderMetricsBackwardsCompatibleConfiguration {

		@Bean
		io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics customClassLoaderMetrics() {
			return new io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics();
		}

	}

}
