/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure;

import java.lang.management.MemoryPoolMXBean;

import io.micrometer.core.instrument.binder.MeterConvention;
import io.micrometer.core.instrument.binder.SimpleMeterConvention;
import io.micrometer.core.instrument.binder.jvm.convention.JvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.JvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.micrometer.MicrometerJvmThreadMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmClassLoadingMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmCpuMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmMemoryMeterConventions;
import io.micrometer.core.instrument.binder.jvm.convention.otel.OpenTelemetryJvmThreadMeterConventions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SemanticConventionAutoConfiguration}.
 */
class SemanticConventionAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SemanticConventionAutoConfiguration.class));

	@Test
	void registersMicrometerConventionsByDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(MicrometerJvmMemoryMeterConventions.class);
			assertThat(context).hasSingleBean(MicrometerJvmClassLoadingMeterConventions.class);
			assertThat(context).hasSingleBean(MicrometerJvmCpuMeterConventions.class);
			assertThat(context).hasSingleBean(MicrometerJvmThreadMeterConventions.class);
			assertThat(context).doesNotHaveBean(OpenTelemetryJvmMemoryMeterConventions.class);
			assertThat(context).doesNotHaveBean(OpenTelemetryJvmClassLoadingMeterConventions.class);
			assertThat(context).doesNotHaveBean(OpenTelemetryJvmCpuMeterConventions.class);
			assertThat(context).doesNotHaveBean(OpenTelemetryJvmThreadMeterConventions.class);
		});
	}

	@Test
	void registersOpenTelemetryConventionsWhenConventionsSetToOpenTelemetry() {
		this.contextRunner.withPropertyValues("management.observations.conventions=opentelemetry").run((context) -> {
			assertThat(context).hasSingleBean(JvmMemoryMeterConventions.class)
				.hasSingleBean(OpenTelemetryJvmMemoryMeterConventions.class);
			assertThat(context).hasSingleBean(JvmClassLoadingMeterConventions.class)
				.hasSingleBean(OpenTelemetryJvmClassLoadingMeterConventions.class);
			assertThat(context).hasSingleBean(JvmCpuMeterConventions.class)
				.hasSingleBean(OpenTelemetryJvmCpuMeterConventions.class);
			assertThat(context).hasSingleBean(JvmThreadMeterConventions.class)
				.hasSingleBean(OpenTelemetryJvmThreadMeterConventions.class);
		});
	}

	@Test
	void allowsCustomMicrometerConventionsToBeUsed() {
		this.contextRunner.withPropertyValues("management.observations.conventions=micrometer")
			.withUserConfiguration(CustomJvmMemoryMeterConventionsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JvmMemoryMeterConventions.class)
					.hasBean("customJvmMemoryMeterConventions");
				assertThat(context).doesNotHaveBean(MicrometerJvmMemoryMeterConventions.class);
				assertThat(context).hasSingleBean(MicrometerJvmClassLoadingMeterConventions.class);
				assertThat(context).hasSingleBean(MicrometerJvmCpuMeterConventions.class);
				assertThat(context).hasSingleBean(MicrometerJvmThreadMeterConventions.class);
			});
	}

	@Test
	void allowsCustomOpenTelemetryConventionsToBeUsed() {
		this.contextRunner.withPropertyValues("management.observations.conventions=opentelemetry")
			.withUserConfiguration(CustomJvmClassLoadingMeterConventionsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JvmClassLoadingMeterConventions.class)
					.hasBean("customJvmClassLoadingMeterConventions");
				assertThat(context).doesNotHaveBean(MicrometerJvmClassLoadingMeterConventions.class);
				assertThat(context).hasSingleBean(OpenTelemetryJvmMemoryMeterConventions.class);
				assertThat(context).hasSingleBean(OpenTelemetryJvmCpuMeterConventions.class);
				assertThat(context).hasSingleBean(OpenTelemetryJvmThreadMeterConventions.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmMemoryMeterConventionsConfiguration {

		@Bean
		JvmMemoryMeterConventions customJvmMemoryMeterConventions() {
			return new JvmMemoryMeterConventions() {
				@Override
				public MeterConvention<MemoryPoolMXBean> getMemoryUsedConvention() {
					return new SimpleMeterConvention<>("my.memory.used");
				}

				@Override
				public MeterConvention<MemoryPoolMXBean> getMemoryCommittedConvention() {
					return new SimpleMeterConvention<>("my.memory.committed");
				}

				@Override
				public MeterConvention<MemoryPoolMXBean> getMemoryMaxConvention() {
					return new SimpleMeterConvention<>("my.memory.max");
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJvmClassLoadingMeterConventionsConfiguration {

		@Bean
		JvmClassLoadingMeterConventions customJvmClassLoadingMeterConventions() {
			return new JvmClassLoadingMeterConventions() {
				@Override
				public MeterConvention<Object> loadedConvention() {
					return new SimpleMeterConvention<>("my.classes.loaded");
				}

				@Override
				public MeterConvention<Object> unloadedConvention() {
					return new SimpleMeterConvention<>("my.classes.unloaded");
				}

				@Override
				public MeterConvention<Object> currentClassCountConvention() {
					return new SimpleMeterConvention<>("my.classes.current");
				}
			};
		}

	}

}
