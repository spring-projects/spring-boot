/*
 * Copyright 2012-2023 the original author or authors.
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
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NoopTracerAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class NoopTracerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(NoopTracerAutoConfiguration.class));

	@Test
	void shouldSupplyNoopTracer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Tracer.class);
			Tracer tracer = context.getBean(Tracer.class);
			assertThat(tracer).isEqualTo(Tracer.NOOP);
		});
	}

	@Test
	void shouldBackOffOnCustomTracer() {
		this.contextRunner.withUserConfiguration(CustomTracerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Tracer.class);
			assertThat(context).hasBean("customTracer");
			Tracer tracer = context.getBean(Tracer.class);
			assertThat(tracer).isNotEqualTo(Tracer.NOOP);
		});
	}

	@Test
	void shouldBackOffIfMicrometerTracingIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.tracing"))
			.run((context) -> assertThat(context).doesNotHaveBean(Tracer.class));

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomTracerConfiguration {

		@Bean
		Tracer customTracer() {
			return mock(Tracer.class);
		}

	}

}
