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

package org.springframework.boot.micrometer.tracing.autoconfigure.otlp;

import io.micrometer.registry.otlp.ExemplarContextProvider;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.OtlpExemplarsAutoConfiguration.LazyTracingExemplarContextProvider;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OtlpExemplarsAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class OtlpExemplarsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OtlpExemplarsAutoConfiguration.class));

	@Test
	void shouldSupplyExemplarContextProvider() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ExemplarContextProvider.class);
			assertThat(context).hasSingleBean(LazyTracingExemplarContextProvider.class);
		});
	}

	@Test
	void shouldBackOffOnCustomExemplarContextProvider() {
		this.contextRunner
			.withUserConfiguration(TracerConfiguration.class, CustomExemplarContextProviderConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ExemplarContextProvider.class);
				assertThat(context).doesNotHaveBean(LazyTracingExemplarContextProvider.class);
				assertThat(context).hasBean("customExemplarContextProvider");
			});
	}

	@Test
	void shouldNotSupplyExemplarContextProviderWhenTracerBeanIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ExemplarContextProvider.class));
	}

	@Test
	void shouldNotSupplyExemplarContextProviderWhenTracerClassIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Tracer.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ExemplarContextProvider.class));
	}

	@Test
	void shouldNotSupplyExemplarContextProviderWhenExemplarContextProviderClassIsMissing() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withClassLoader(new FilteredClassLoader(ExemplarContextProvider.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ExemplarContextProvider.class));
	}

	@Test
	void shouldNotSupplyExemplarContextProviderWhenIncludeIsNone() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withPropertyValues("management.tracing.exemplars.include=none")
			.run((context) -> assertThat(context).doesNotHaveBean(ExemplarContextProvider.class));
	}

	@Test
	void shouldSupplyExemplarContextProviderWhenIncludeIsSampledTraces() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withPropertyValues("management.tracing.exemplars.include=sampled-traces")
			.run((context) -> assertThat(context).hasSingleBean(ExemplarContextProvider.class));
	}

	@Test
	void shouldSupplyExemplarContextProviderWhenIncludeIsAll() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
			.withPropertyValues("management.tracing.exemplars.include=all")
			.run((context) -> assertThat(context).hasSingleBean(ExemplarContextProvider.class));
	}

	@Configuration(proxyBeanMethods = false)
	private static final class TracerConfiguration {

		@Bean
		Tracer tracer() {
			return mock(Tracer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomExemplarContextProviderConfiguration {

		@Bean
		ExemplarContextProvider customExemplarContextProvider() {
			return mock(ExemplarContextProvider.class);
		}

	}

}
