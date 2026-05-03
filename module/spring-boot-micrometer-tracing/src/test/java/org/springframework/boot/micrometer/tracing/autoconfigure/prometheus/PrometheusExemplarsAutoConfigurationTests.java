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

package org.springframework.boot.micrometer.tracing.autoconfigure.prometheus;

import io.micrometer.tracing.Tracer;
import io.prometheus.metrics.tracer.common.SpanContext;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.micrometer.tracing.autoconfigure.prometheus.PrometheusExemplarsAutoConfiguration.LazyTracingSpanContext;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrometheusExemplarsAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class PrometheusExemplarsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PrometheusExemplarsAutoConfiguration.class));

	@Test
	void shouldSupplySpanContext() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(SpanContext.class);
			assertThat(context).hasSingleBean(LazyTracingSpanContext.class);
		});
	}

	@Test
	void shouldBackOffOnCustomSpanContext() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class, CustomSpanContextConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(SpanContext.class);
				assertThat(context).doesNotHaveBean(LazyTracingSpanContext.class);
				assertThat(context).hasBean("customSpanContext");
			});
	}

	@Test
	void shouldNotSupplySpanContextWhenTracerBeanIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(SpanContext.class));
	}

	@Test
	void shouldNotSupplySpanContextWhenTracerClassIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Tracer.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContext.class));
	}

	@Test
	void shouldNotSupplySpanContextWhenSpanContextClassIsMissing() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withClassLoader(new FilteredClassLoader(SpanContext.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContext.class));
	}

	@Test
	void shouldNotSupplySpanContextWhenIncludeIsNone() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withPropertyValues("management.tracing.exemplars.include=none")
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContext.class));
	}

	@Test
	void shouldFailWhenIncludeIsAll() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withPropertyValues("management.tracing.exemplars.include=all")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(InvalidConfigurationPropertyValueException.class)
				.hasMessageContaining(
						"Property management.tracing.exemplars.include with value 'all' is invalid: Prometheus doesn't support including 'all' traces as exemplars."));
	}

	@Test
	void shouldSupplySpanContextWhenIncludeIsSampledTraces() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withPropertyValues("management.tracing.exemplars.include=sampled-traces")
			.run((context) -> assertThat(context).hasSingleBean(SpanContext.class));
	}

	@Configuration(proxyBeanMethods = false)
	private static final class TracerConfiguration {

		@Bean
		Tracer tracer() {
			return mock(Tracer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomSpanContextConfiguration {

		@Bean
		SpanContext customSpanContext() {
			return mock(SpanContext.class);
		}

	}

}
