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

import java.util.List;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class MetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class));

	@Test
	public void autoConfiguresAClock() {
		this.contextRunner
				.run((context) -> assertThat(context).hasSingleBean(Clock.class));
	}

	@Test
	public void allowsACustomClockToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomClockConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasBean("customClock"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void configuresMeterRegistries() {
		this.contextRunner.withUserConfiguration(MeterRegistryConfiguration.class)
				.run((context) -> {
					MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
					List<MeterFilter> filters = (List<MeterFilter>) ReflectionTestUtils
							.getField(meterRegistry, "filters");
					assertThat(filters).isNotEmpty();
					verify((MeterBinder) context.getBean("meterBinder"))
							.bindTo(meterRegistry);
					verify(context.getBean(MeterRegistryCustomizer.class))
							.customize(meterRegistry);
				});
	}

	@Test
	public void autoConfiguresJvmMetrics() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(JvmGcMetrics.class).hasSingleBean(JvmMemoryMetrics.class)
				.hasSingleBean(JvmThreadMetrics.class)
				.hasSingleBean(ClassLoaderMetrics.class));
	}

	@Test
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

	@Test
	public void autoConfiguresLogbackMetrics() {
		this.contextRunner.run(
				(context) -> assertThat(context).hasSingleBean(LogbackMetrics.class));
	}

	@Test
	public void allowsLogbackMetricsToBeDisabled() {
		this.contextRunner
				.withPropertyValues("management.metrics.binders.logback.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(LogbackMetrics.class));
	}

	@Test
	public void allowsCustomLogbackMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomLogbackMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(LogbackMetrics.class)
						.hasBean("customLogbackMetrics"));
	}

	@Test
	public void autoConfiguresUptimeMetrics() {
		this.contextRunner
				.run((context) -> assertThat(context).hasSingleBean(UptimeMetrics.class));
	}

	@Test
	public void allowsUptimeMetricsToBeDisabled() {
		this.contextRunner
				.withPropertyValues("management.metrics.binders.uptime.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(UptimeMetrics.class));
	}

	@Test
	public void allowsCustomUptimeMetricsToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomUptimeMetricsConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(UptimeMetrics.class)
						.hasBean("customUptimeMetrics"));
	}

	@Test
	public void autoConfiguresProcessorMetrics() {
		this.contextRunner.run(
				(context) -> assertThat(context).hasSingleBean(ProcessorMetrics.class));
	}

	@Test
	public void allowsProcessorMetricsToBeDisabled() {
		this.contextRunner
				.withPropertyValues("management.metrics.binders.processor.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(ProcessorMetrics.class));
	}

	@Test
	public void allowsCustomProcessorMetricsToBeUsed() {
		this.contextRunner
				.withUserConfiguration(CustomProcessorMetricsConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(ProcessorMetrics.class)
						.hasBean("customProcessorMetrics"));
	}

	@Test
	public void autoConfiguresFileDescriptorMetrics() {
		this.contextRunner.run((context) -> assertThat(context)
				.hasSingleBean(FileDescriptorMetrics.class));
	}

	@Test
	public void allowsFileDescriptorMetricsToBeDisabled() {
		this.contextRunner
				.withPropertyValues("management.metrics.binders.files.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(FileDescriptorMetrics.class));
	}

	@Test
	public void allowsCustomFileDescriptorMetricsToBeUsed() {
		this.contextRunner
				.withUserConfiguration(CustomFileDescriptorMetricsConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(FileDescriptorMetrics.class)
						.hasBean("customFileDescriptorMetrics"));
	}

	@Configuration
	static class CustomClockConfiguration {

		@Bean
		Clock customClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration
	static class MeterRegistryConfiguration {

		@Bean
		MeterRegistry meterRegistry() {
			SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
			return spy(meterRegistry);
		}

		@Bean
		@SuppressWarnings("rawtypes")
		MeterRegistryCustomizer meterRegistryCustomizer() {
			return mock(MeterRegistryCustomizer.class);
		}

		@Bean
		MeterBinder meterBinder() {
			return mock(MeterBinder.class);
		}

	}

	@Configuration
	static class CustomJvmGcMetricsConfiguration {

		@Bean
		JvmGcMetrics customJvmGcMetrics() {
			return new JvmGcMetrics();
		}

	}

	@Configuration
	static class CustomJvmMemoryMetricsConfiguration {

		@Bean
		JvmMemoryMetrics customJvmMemoryMetrics() {
			return new JvmMemoryMetrics();
		}

	}

	@Configuration
	static class CustomJvmThreadMetricsConfiguration {

		@Bean
		JvmThreadMetrics customJvmThreadMetrics() {
			return new JvmThreadMetrics();
		}

	}

	@Configuration
	static class CustomClassLoaderMetricsConfiguration {

		@Bean
		ClassLoaderMetrics customClassLoaderMetrics() {
			return new ClassLoaderMetrics();
		}

	}

	@Configuration
	static class CustomLogbackMetricsConfiguration {

		@Bean
		LogbackMetrics customLogbackMetrics() {
			return new LogbackMetrics();
		}

	}

	@Configuration
	static class CustomUptimeMetricsConfiguration {

		@Bean
		UptimeMetrics customUptimeMetrics() {
			return new UptimeMetrics();
		}

	}

	@Configuration
	static class CustomProcessorMetricsConfiguration {

		@Bean
		ProcessorMetrics customProcessorMetrics() {
			return new ProcessorMetrics();
		}

	}

	@Configuration
	static class CustomFileDescriptorMetricsConfiguration {

		@Bean
		FileDescriptorMetrics customFileDescriptorMetrics() {
			return new FileDescriptorMetrics();
		}

	}

}
