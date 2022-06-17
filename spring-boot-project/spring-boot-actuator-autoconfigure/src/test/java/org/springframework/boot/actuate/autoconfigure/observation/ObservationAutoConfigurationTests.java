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

package org.springframework.boot.actuate.autoconfigure.observation;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.Observation.GlobalKeyValuesProvider;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.AllMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ObservationAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withClassLoader(new FilteredClassLoader("io.micrometer.tracing"))
			.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class));

	private final ApplicationContextRunner tracingContextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple()).withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class));

	@Test
	void autoConfiguresTimerObservationHandler() {
		this.contextRunner.run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test-observation", observationRegistry).stop();
			// When a TimerObservationHandler is registered, every stopped Observation
			// leads to a timer
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThat(meterRegistry.get("test-observation").timer().count()).isEqualTo(1);
		});
	}

	@Test
	void allowsTimerObservationHandlerToBeDisabled() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
				.run((context) -> assertThat(context)
						.doesNotHaveBean(TimerObservationHandlerObservationRegistryCustomizer.class));
	}

	@Test
	void autoConfiguresObservationPredicates() {
		this.contextRunner.withUserConfiguration(ObservationPredicates.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			// This is allowed by ObservationPredicates.customPredicate
			Observation.start("observation1", observationRegistry).start().stop();
			// This isn't allowed by ObservationPredicates.customPredicate
			Observation.start("observation2", observationRegistry).start().stop();
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThat(meterRegistry.get("observation1").timer().count()).isEqualTo(1);
			assertThatThrownBy(() -> meterRegistry.get("observation2").timer())
					.isInstanceOf(MeterNotFoundException.class);
		});
	}

	@Test
	void autoConfiguresGlobalKeyValuesProvider() {
		this.contextRunner.withUserConfiguration(GlobalKeyValuesProviders.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Context micrometerContext = new Context();
			Observation.start("test-observation", micrometerContext, observationRegistry).stop();
			assertThat(micrometerContext.getAllKeyValues()).containsExactly(KeyValue.of("key1", "value1"));
		});
	}

	@Test
	void autoConfiguresObservationHandler() {
		this.contextRunner.withUserConfiguration(ObservationHandlers.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			List<ObservationHandler<?>> handlers = context.getBean(CalledHandlers.class).getCalledHandlers();
			Observation.start("test-observation", observationRegistry);
			assertThat(handlers).hasSize(2);
			// Regular handlers are registered first
			assertThat(handlers.get(0)).isInstanceOf(CustomObservationHandler.class);
			// Multiple MeterObservationHandler are wrapped in
			// FirstMatchingCompositeObservationHandler, which calls only the first
			// one
			assertThat(handlers.get(1)).isInstanceOf(CustomMeterObservationHandler.class);
			assertThat(((CustomMeterObservationHandler) handlers.get(1)).getName())
					.isEqualTo("customMeterObservationHandler1");
		});
	}

	@Test
	void autoConfiguresObservationHandlerWithCustomContext() {
		this.contextRunner.withUserConfiguration(ObservationHandlerWithCustomContextConfiguration.class)
				.run((context) -> {
					ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
					List<ObservationHandler<?>> handlers = context.getBean(CalledHandlers.class).getCalledHandlers();
					CustomContext customContext = new CustomContext();
					Observation.start("test-observation", customContext, observationRegistry);
					assertThat(handlers).hasSize(1);
					assertThat(handlers.get(0)).isInstanceOf(ObservationHandlerWithCustomContext.class);
				});
	}

	@Test
	void autoConfiguresObservationHandlerWhenTracingIsActive() {
		this.tracingContextRunner.withUserConfiguration(ObservationHandlersTracing.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			List<ObservationHandler<?>> handlers = context.getBean(CalledHandlers.class).getCalledHandlers();
			Observation.start("test-observation", observationRegistry);
			assertThat(handlers).hasSize(3);
			// Regular handlers are registered first
			assertThat(handlers.get(0)).isInstanceOf(CustomObservationHandler.class);
			// Multiple MeterObservationHandler are wrapped in
			// FirstMatchingCompositeObservationHandler, which calls only the first
			// one
			assertThat(handlers.get(1)).isInstanceOf(CustomMeterObservationHandler.class);
			assertThat(((CustomMeterObservationHandler) handlers.get(1)).getName())
					.isEqualTo("customMeterObservationHandler1");
			// Multiple TracingObservationHandler are wrapped in
			// FirstMatchingCompositeObservationHandler, which calls only the first
			// one
			assertThat(handlers.get(2)).isInstanceOf(CustomTracingObservationHandler.class);
			assertThat(((CustomTracingObservationHandler) handlers.get(2)).getName())
					.isEqualTo("customTracingHandler1");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class ObservationPredicates {

		@Bean
		ObservationPredicate customPredicate() {
			return (s, context) -> s.equals("observation1");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GlobalKeyValuesProviders {

		@Bean
		Observation.GlobalKeyValuesProvider<?> customKeyValuesProvider() {
			return new GlobalKeyValuesProvider<>() {
				@Override
				public boolean supportsContext(Context context) {
					return true;
				}

				@Override
				public KeyValues getLowCardinalityKeyValues(Context context) {
					return KeyValues.of("key1", "value1");
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(CalledHandlersConfiguration.class)
	static class ObservationHandlers {

		@Bean
		@Order(4)
		AllMatchingCompositeObservationHandler customAllMatchingCompositeObservationHandler() {
			return new AllMatchingCompositeObservationHandler();
		}

		@Bean
		@Order(3)
		FirstMatchingCompositeObservationHandler customFirstMatchingCompositeObservationHandler() {
			return new FirstMatchingCompositeObservationHandler();
		}

		@Bean
		@Order(2)
		ObservationHandler<Context> customObservationHandler(CalledHandlers calledHandlers) {
			return new CustomObservationHandler(calledHandlers);
		}

		@Bean
		@Order(1)
		MeterObservationHandler<Context> customMeterObservationHandler2(CalledHandlers calledHandlers) {
			return new CustomMeterObservationHandler("customMeterObservationHandler2", calledHandlers);
		}

		@Bean
		@Order(0)
		MeterObservationHandler<Context> customMeterObservationHandler1(CalledHandlers calledHandlers) {
			return new CustomMeterObservationHandler("customMeterObservationHandler1", calledHandlers);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(CalledHandlersConfiguration.class)
	static class ObservationHandlerWithCustomContextConfiguration {

		@Bean
		ObservationHandlerWithCustomContext observationHandlerWithCustomContext(CalledHandlers calledHandlers) {
			return new ObservationHandlerWithCustomContext(calledHandlers);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(CalledHandlersConfiguration.class)
	static class ObservationHandlersTracing {

		@Bean
		@Order(6)
		CustomTracingObservationHandler customTracingHandler2(CalledHandlers calledHandlers) {
			return new CustomTracingObservationHandler("customTracingHandler2", calledHandlers);
		}

		@Bean
		@Order(5)
		CustomTracingObservationHandler customTracingHandler1(CalledHandlers calledHandlers) {
			return new CustomTracingObservationHandler("customTracingHandler1", calledHandlers);
		}

		@Bean
		@Order(4)
		AllMatchingCompositeObservationHandler customAllMatchingCompositeObservationHandler() {
			return new AllMatchingCompositeObservationHandler();
		}

		@Bean
		@Order(3)
		FirstMatchingCompositeObservationHandler customFirstMatchingCompositeObservationHandler() {
			return new FirstMatchingCompositeObservationHandler();
		}

		@Bean
		@Order(2)
		ObservationHandler<Context> customObservationHandler(CalledHandlers calledHandlers) {
			return new CustomObservationHandler(calledHandlers);
		}

		@Bean
		@Order(1)
		MeterObservationHandler<Context> customMeterObservationHandler2(CalledHandlers calledHandlers) {
			return new CustomMeterObservationHandler("customMeterObservationHandler2", calledHandlers);
		}

		@Bean
		@Order(0)
		MeterObservationHandler<Context> customMeterObservationHandler1(CalledHandlers calledHandlers) {
			return new CustomMeterObservationHandler("customMeterObservationHandler1", calledHandlers);
		}

	}

	private static class CustomTracingObservationHandler implements TracingObservationHandler<Context> {

		private final Tracer tracer = mock(Tracer.class, Answers.RETURNS_MOCKS);

		private final String name;

		private final CalledHandlers calledHandlers;

		CustomTracingObservationHandler(String name, CalledHandlers calledHandlers) {
			this.name = name;
			this.calledHandlers = calledHandlers;
		}

		String getName() {
			return this.name;
		}

		@Override
		public Tracer getTracer() {
			return this.tracer;
		}

		@Override
		public void onStart(Context context) {
			this.calledHandlers.onCalled(this);
		}

		@Override
		public boolean supportsContext(Context context) {
			return true;
		}

	}

	private static class ObservationHandlerWithCustomContext implements ObservationHandler<CustomContext> {

		private final CalledHandlers calledHandlers;

		ObservationHandlerWithCustomContext(CalledHandlers calledHandlers) {
			this.calledHandlers = calledHandlers;
		}

		@Override
		public void onStart(CustomContext context) {
			this.calledHandlers.onCalled(this);
		}

		@Override
		public boolean supportsContext(Context context) {
			return context instanceof CustomContext;
		}

	}

	private static class CustomContext extends Context {

	}

	private static class CalledHandlers {

		private final List<ObservationHandler<?>> calledHandlers = new ArrayList<>();

		void onCalled(ObservationHandler<?> handler) {
			this.calledHandlers.add(handler);
		}

		List<ObservationHandler<?>> getCalledHandlers() {
			return this.calledHandlers;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CalledHandlersConfiguration {

		@Bean
		CalledHandlers calledHandlers() {
			return new CalledHandlers();
		}

	}

	private static class CustomObservationHandler implements ObservationHandler<Context> {

		private final CalledHandlers calledHandlers;

		CustomObservationHandler(CalledHandlers calledHandlers) {
			this.calledHandlers = calledHandlers;
		}

		@Override
		public void onStart(Context context) {
			this.calledHandlers.onCalled(this);
		}

		@Override
		public boolean supportsContext(Context context) {
			return true;
		}

	}

	private static class CustomMeterObservationHandler implements MeterObservationHandler<Context> {

		private final CalledHandlers calledHandlers;

		private final String name;

		CustomMeterObservationHandler(String name, CalledHandlers calledHandlers) {
			this.name = name;
			this.calledHandlers = calledHandlers;
		}

		String getName() {
			return this.name;
		}

		@Override
		public void onStart(Context context) {
			this.calledHandlers.onCalled(this);
		}

		@Override
		public boolean supportsContext(Context context) {
			return true;
		}

	}

}
