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
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryConfigurations.SdkConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SdkConfiguration}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryConfigurationsSdkConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SdkConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasSingleBean(SdkTracerProvider.class);
			assertThat(context).hasSingleBean(ContextPropagators.class);
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasSingleBean(SpanProcessor.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomBeans.class).run((context) -> {
			assertThat(context).hasBean("customOpenTelemetry");
			assertThat(context).hasSingleBean(OpenTelemetry.class);
			assertThat(context).hasBean("customSdkTracerProvider");
			assertThat(context).hasSingleBean(SdkTracerProvider.class);
			assertThat(context).hasBean("customContextPropagators");
			assertThat(context).hasSingleBean(ContextPropagators.class);
			assertThat(context).hasBean("customSampler");
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasBean("customSpanProcessor");
			assertThat(context).hasSingleBean(SpanProcessor.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfSdkIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.sdk")).run((context) -> {
			assertThat(context).doesNotHaveBean(OpenTelemetry.class);
			assertThat(context).doesNotHaveBean(SdkTracerProvider.class);
			assertThat(context).doesNotHaveBean(ContextPropagators.class);
			assertThat(context).doesNotHaveBean(Sampler.class);
			assertThat(context).doesNotHaveBean(SpanProcessor.class);
		});
	}

	private static class CustomBeans {

		@Bean
		OpenTelemetry customOpenTelemetry() {
			return mock(OpenTelemetry.class);
		}

		@Bean
		SdkTracerProvider customSdkTracerProvider() {
			return SdkTracerProvider.builder().build();
		}

		@Bean
		ContextPropagators customContextPropagators() {
			return mock(ContextPropagators.class);
		}

		@Bean
		Sampler customSampler() {
			return mock(Sampler.class);
		}

		@Bean
		SpanProcessor customSpanProcessor() {
			return mock(SpanProcessor.class);
		}

	}

}
