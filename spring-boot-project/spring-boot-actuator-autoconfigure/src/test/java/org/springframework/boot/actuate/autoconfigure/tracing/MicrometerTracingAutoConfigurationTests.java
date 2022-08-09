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

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MicrometerTracingAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class MicrometerTracingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MicrometerTracingAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(DefaultTracingObservationHandler.class));
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customDefaultTracingObservationHandler");
			assertThat(context).hasSingleBean(DefaultTracingObservationHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer"))
				.run((context) -> assertThat(context).doesNotHaveBean(DefaultTracingObservationHandler.class));
	}

	@Test
	void shouldNotSupplyBeansIfTracerIsMissing() {
		this.contextRunner.withUserConfiguration()
				.run((context) -> assertThat(context).doesNotHaveBean(DefaultTracingObservationHandler.class));
	}

	@Test
	void shouldNotSupplyBeansIfTracingIsDisabled() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class)
				.withPropertyValues("management.tracing.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(DefaultTracingObservationHandler.class));
	}

	@Configuration(proxyBeanMethods = false)
	private static class TracerConfiguration {

		@Bean
		Tracer tracer() {
			return mock(Tracer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		DefaultTracingObservationHandler customDefaultTracingObservationHandler() {
			return mock(DefaultTracingObservationHandler.class);
		}

	}

}
