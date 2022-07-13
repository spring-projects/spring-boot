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

package org.springframework.boot.actuate.autoconfigure.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryConfigurations.TracerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TracerConfiguration}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryConfigurationsTracerConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TracerConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(OpenTelemetryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Tracer.class));
	}

	@Test
	void shouldNotSupplyBeansIfApiIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.api"))
				.run((context) -> assertThat(context).doesNotHaveBean(Tracer.class));
	}

	@Test
	void shouldNotSupplyTracerIfOpenTelemetryIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Tracer.class));
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(OpenTelemetryConfiguration.class, CustomConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("customTracer");
					assertThat(context).hasSingleBean(Tracer.class);
				});
	}

	@Configuration(proxyBeanMethods = false)
	private static class OpenTelemetryConfiguration {

		@Bean
		OpenTelemetry openTelemetry() {
			return mock(OpenTelemetry.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		Tracer customTracer() {
			return mock(Tracer.class);
		}

	}

}
