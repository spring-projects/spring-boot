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

import java.util.List;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
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
		this.contextRunner.withUserConfiguration(TracerConfiguration.class, PropagatorConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(DefaultTracingObservationHandler.class);
				assertThat(context).hasSingleBean(PropagatingReceiverTracingObservationHandler.class);
				assertThat(context).hasSingleBean(PropagatingSenderTracingObservationHandler.class);
			});
	}

	@Test
	@SuppressWarnings("rawtypes")
	void shouldSupplyBeansInCorrectOrder() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class, PropagatorConfiguration.class)
			.run((context) -> {
				List<TracingObservationHandler> tracingObservationHandlers = context
					.getBeanProvider(TracingObservationHandler.class)
					.orderedStream()
					.toList();
				assertThat(tracingObservationHandlers).hasSize(3);
				assertThat(tracingObservationHandlers.get(0))
					.isInstanceOf(PropagatingReceiverTracingObservationHandler.class);
				assertThat(tracingObservationHandlers.get(1))
					.isInstanceOf(PropagatingSenderTracingObservationHandler.class);
				assertThat(tracingObservationHandlers.get(2)).isInstanceOf(DefaultTracingObservationHandler.class);
			});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customDefaultTracingObservationHandler");
			assertThat(context).hasSingleBean(DefaultTracingObservationHandler.class);
			assertThat(context).hasBean("customPropagatingReceiverTracingObservationHandler");
			assertThat(context).hasSingleBean(PropagatingReceiverTracingObservationHandler.class);
			assertThat(context).hasBean("customPropagatingSenderTracingObservationHandler");
			assertThat(context).hasSingleBean(PropagatingSenderTracingObservationHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer")).run((context) -> {
			assertThat(context).doesNotHaveBean(DefaultTracingObservationHandler.class);
			assertThat(context).doesNotHaveBean(PropagatingReceiverTracingObservationHandler.class);
			assertThat(context).doesNotHaveBean(PropagatingSenderTracingObservationHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfTracerIsMissing() {
		this.contextRunner.withUserConfiguration(PropagatorConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(DefaultTracingObservationHandler.class);
			assertThat(context).doesNotHaveBean(PropagatingReceiverTracingObservationHandler.class);
			assertThat(context).doesNotHaveBean(PropagatingSenderTracingObservationHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBeansIfPropagatorIsMissing() {
		this.contextRunner.withUserConfiguration(TracerConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(PropagatingSenderTracingObservationHandler.class);
			assertThat(context).doesNotHaveBean(PropagatingReceiverTracingObservationHandler.class);
		});
	}

	@Configuration(proxyBeanMethods = false)
	private static class TracerConfiguration {

		@Bean
		Tracer tracer() {
			return mock(Tracer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class PropagatorConfiguration {

		@Bean
		Propagator propagator() {
			return mock(Propagator.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		DefaultTracingObservationHandler customDefaultTracingObservationHandler() {
			return mock(DefaultTracingObservationHandler.class);
		}

		@Bean
		PropagatingReceiverTracingObservationHandler<?> customPropagatingReceiverTracingObservationHandler() {
			return mock(PropagatingReceiverTracingObservationHandler.class);
		}

		@Bean
		PropagatingSenderTracingObservationHandler<?> customPropagatingSenderTracingObservationHandler() {
			return mock(PropagatingSenderTracingObservationHandler.class);
		}

	}

}
