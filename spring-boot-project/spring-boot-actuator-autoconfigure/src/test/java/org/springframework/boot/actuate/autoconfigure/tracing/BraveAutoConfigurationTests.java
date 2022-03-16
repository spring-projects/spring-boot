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

import brave.Tracer;
import brave.Tracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation.Factory;
import brave.sampler.Sampler;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BraveAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class BraveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class));

	@Test
	void shouldSupplyBraveBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Tracing.class);
			assertThat(context).hasSingleBean(Tracer.class);
			assertThat(context).hasSingleBean(CurrentTraceContext.class);
			assertThat(context).hasSingleBean(Factory.class);
			assertThat(context).hasSingleBean(Sampler.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBraveBeans() {
		this.contextRunner.withUserConfiguration(CustomBraveConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customTracing");
			assertThat(context).hasSingleBean(Tracing.class);
			assertThat(context).hasBean("customTracer");
			assertThat(context).hasSingleBean(Tracer.class);
			assertThat(context).hasBean("customCurrentTraceContext");
			assertThat(context).hasSingleBean(CurrentTraceContext.class);
			assertThat(context).hasBean("customFactory");
			assertThat(context).hasSingleBean(Factory.class);
			assertThat(context).hasBean("customSampler");
			assertThat(context).hasSingleBean(Sampler.class);
		});
	}

	@Test
	void shouldSupplyMicrometerBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BraveTracer.class);
			assertThat(context).hasSingleBean(BraveBaggageManager.class);
		});
	}

	@Test
	void shouldBackOffOnCustomMicrometerBraveBeans() {
		this.contextRunner.withUserConfiguration(CustomMicrometerBraveConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customBraveTracer");
			assertThat(context).hasSingleBean(BraveTracer.class);
			assertThat(context).hasBean("customBraveBaggageManager");
			assertThat(context).hasSingleBean(BraveBaggageManager.class);
		});
	}

	@Test
	void shouldNotSupplyBraveBeansIfBraveIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("brave")).run((context) -> {
			assertThat(context).doesNotHaveBean(Tracing.class);
			assertThat(context).doesNotHaveBean(Tracer.class);
			assertThat(context).doesNotHaveBean(CurrentTraceContext.class);
			assertThat(context).doesNotHaveBean(Factory.class);
			assertThat(context).doesNotHaveBean(Sampler.class);
		});
	}

	@Test
	void shouldNotSupplyMicrometerBeansIfMicrometerIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer")).run((context) -> {
			assertThat(context).doesNotHaveBean(BraveTracer.class);
			assertThat(context).doesNotHaveBean(BraveBaggageManager.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomBraveConfiguration {

		@Bean
		Tracing customTracing() {
			return Mockito.mock(Tracing.class);
		}

		@Bean
		Tracer customTracer() {
			return Mockito.mock(Tracer.class);
		}

		@Bean
		CurrentTraceContext customCurrentTraceContext() {
			return Mockito.mock(CurrentTraceContext.class);
		}

		@Bean
		Factory customFactory() {
			return Mockito.mock(Factory.class);
		}

		@Bean
		Sampler customSampler() {
			return Mockito.mock(Sampler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomMicrometerBraveConfiguration {

		@Bean
		BraveTracer customBraveTracer() {
			return Mockito.mock(BraveTracer.class);
		}

		@Bean
		BraveBaggageManager customBraveBaggageManager() {
			return Mockito.mock(BraveBaggageManager.class);
		}

	}

}
