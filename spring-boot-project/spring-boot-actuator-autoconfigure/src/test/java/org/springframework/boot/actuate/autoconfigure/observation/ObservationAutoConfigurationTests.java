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

package org.springframework.boot.actuate.autoconfigure.observation;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.observation.GlobalObservationConvention;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.AllMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.aspectj.weaver.Advice;
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
 * @author Jonatan Ivanov
 * @author Vedran Pavic
 */
class ObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withClassLoader(new FilteredClassLoader("io.micrometer.tracing"))
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class));

	private final ApplicationContextRunner tracingContextRunner = new ApplicationContextRunner()
		.with(MetricsRun.simple())
		.withUserConfiguration(TracerConfiguration.class)
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class));

	@Test
	void beansShouldNotBeSuppliedWhenMicrometerObservationIsNotOnClassPath() {
		this.tracingContextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.observation"))
			.run((context) -> {
				assertThat(context).hasSingleBean(MeterRegistry.class);
				assertThat(context).doesNotHaveBean(ObservationRegistry.class);
				assertThat(context).doesNotHaveBean(ObservationHandler.class);
				assertThat(context).doesNotHaveBean(ObservedAspect.class);
				assertThat(context).doesNotHaveBean(ObservationHandlerGrouping.class);
			});
	}

	@Test
	void supplyObservationRegistryWhenMicrometerCoreAndTracingAreNotOnClassPath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.core", "io.micrometer.tracing"))
			.run((context) -> {
				ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
				Observation.start("test-observation", observationRegistry).stop();
				assertThat(context).doesNotHaveBean(ObservationHandler.class);
				assertThat(context).hasSingleBean(ObservedAspect.class);
				assertThat(context).doesNotHaveBean(ObservationHandlerGrouping.class);
			});
	}

	@Test
	void supplyMeterHandlerAndGroupingWhenMicrometerCoreIsOnClassPathButTracingIsNot() {
		this.contextRunner.run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test-observation", observationRegistry).stop();
			assertThat(context).hasSingleBean(ObservationHandler.class);
			assertThat(context).hasSingleBean(DefaultMeterObservationHandler.class);
			assertThat(context).hasSingleBean(ObservedAspect.class);
			assertThat(context).hasSingleBean(ObservationHandlerGrouping.class);
			assertThat(context).hasBean("metricsObservationHandlerGrouping");
		});
	}

	@Test
	void supplyOnlyTracingObservationHandlerGroupingWhenMicrometerCoreIsNotOnClassPathButTracingIs() {
		this.tracingContextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.core")).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test-observation", observationRegistry).stop();
			assertThat(context).doesNotHaveBean(ObservationHandler.class);
			assertThat(context).hasSingleBean(ObservedAspect.class);
			assertThat(context).hasSingleBean(ObservationHandlerGrouping.class);
			assertThat(context).hasBean("tracingObservationHandlerGrouping");
		});
	}

	@Test
	void supplyMeterHandlerAndGroupingWhenMicrometerCoreAndTracingAreOnClassPath() {
		this.tracingContextRunner.run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			// Intentionally not stopped since that will trigger additional logic in
			// TracingAwareMeterObservationHandler that we don't test here
			Observation.start("test-observation", observationRegistry);
			assertThat(context).hasSingleBean(ObservationHandler.class);
			assertThat(context).hasSingleBean(ObservedAspect.class);
			assertThat(context).hasSingleBean(TracingAwareMeterObservationHandler.class);
			assertThat(context).hasSingleBean(ObservationHandlerGrouping.class);
			assertThat(context).hasBean("metricsAndTracingObservationHandlerGrouping");
		});
	}

	@Test
	void supplyMeterHandlerAndGroupingWhenMicrometerCoreAndTracingAreOnClassPathButThereIsNoTracer() {
		new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class))
			.run((context) -> {
				ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
				Observation.start("test-observation", observationRegistry).stop();
				assertThat(context).hasSingleBean(ObservationHandler.class);
				assertThat(context).hasSingleBean(DefaultMeterObservationHandler.class);
				assertThat(context).hasSingleBean(ObservedAspect.class);
				assertThat(context).hasSingleBean(ObservationHandlerGrouping.class);
				assertThat(context).hasBean("metricsAndTracingObservationHandlerGrouping");
			});
	}

	@Test
	void autoConfiguresDefaultMeterObservationHandler() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(DefaultMeterObservationHandler.class);
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test-observation", observationRegistry).stop();
			// When a DefaultMeterObservationHandler is registered, every stopped
			// Observation leads to a timer
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThat(meterRegistry.get("test-observation").timer().count()).isOne();
			assertThat(context).hasSingleBean(DefaultMeterObservationHandler.class);
			assertThat(context).hasSingleBean(ObservationHandler.class);
			assertThat(context).hasSingleBean(ObservedAspect.class);
		});
	}

	@Test
	void allowsDefaultMeterObservationHandlerToBeDisabled() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(MeterRegistry.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ObservationHandler.class));
	}

	@Test
	void allowsObservedAspectToBeDisabled() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Advice.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ObservedAspect.class));
	}

	@Test
	void allowsObservedAspectToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomObservedAspectConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ObservedAspect.class)
				.getBean(ObservedAspect.class)
				.isSameAs(context.getBean("customObservedAspect")));
	}

	@Test
	void autoConfiguresObservationPredicates() {
		this.contextRunner.withUserConfiguration(ObservationPredicates.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			// This is allowed by ObservationPredicates.customPredicate
			Observation.start("observation1", observationRegistry).stop();
			// This isn't allowed by ObservationPredicates.customPredicate
			Observation.start("observation2", observationRegistry).stop();
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThat(meterRegistry.get("observation1").timer().count()).isOne();
			assertThatThrownBy(() -> meterRegistry.get("observation2").timer())
				.isInstanceOf(MeterNotFoundException.class);
		});
	}

	@Test
	void autoConfiguresObservationFilters() {
		this.contextRunner.withUserConfiguration(ObservationFilters.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("filtered", observationRegistry).stop();
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThat(meterRegistry.get("filtered").tag("filter", "one").timer().count()).isOne();
		});
	}

	@Test
	void shouldSupplyPropertiesObservationFilterBean() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(PropertiesObservationFilterPredicate.class));
	}

	@Test
	void shouldApplyCommonKeyValuesToObservations() {
		this.contextRunner.withPropertyValues("management.observations.key-values.a=alpha").run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("keyvalues", observationRegistry).stop();
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThat(meterRegistry.get("keyvalues").tag("a", "alpha").timer().count()).isOne();
		});
	}

	@Test
	void autoConfiguresGlobalObservationConventions() {
		this.contextRunner.withUserConfiguration(CustomGlobalObservationConvention.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Context micrometerContext = new Context();
			Observation.start("test-observation", () -> micrometerContext, observationRegistry).stop();
			assertThat(micrometerContext.getAllKeyValues()).containsExactly(KeyValue.of("key1", "value1"));
		});
	}

	@Test
	void autoConfiguresObservationHandlers() {
		this.contextRunner.withUserConfiguration(ObservationHandlers.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			List<ObservationHandler<?>> handlers = context.getBean(CalledHandlers.class).getCalledHandlers();
			Observation.start("test-observation", observationRegistry).stop();
			assertThat(context).doesNotHaveBean(DefaultMeterObservationHandler.class);
			assertThat(handlers).hasSize(2);
			// Multiple MeterObservationHandler are wrapped in
			// FirstMatchingCompositeObservationHandler, which calls only the first one
			assertThat(handlers.get(0)).isInstanceOf(CustomMeterObservationHandler.class);
			assertThat(((CustomMeterObservationHandler) handlers.get(0)).getName())
				.isEqualTo("customMeterObservationHandler1");
			// Regular handlers are registered last
			assertThat(handlers.get(1)).isInstanceOf(CustomObservationHandler.class);
			assertThat(context).doesNotHaveBean(DefaultMeterObservationHandler.class);
			assertThat(context).doesNotHaveBean(TracingAwareMeterObservationHandler.class);
		});
	}

	@Test
	void autoConfiguresObservationHandlerWithCustomContext() {
		this.contextRunner.withUserConfiguration(ObservationHandlerWithCustomContextConfiguration.class)
			.run((context) -> {
				ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
				List<ObservationHandler<?>> handlers = context.getBean(CalledHandlers.class).getCalledHandlers();
				CustomContext customContext = new CustomContext();
				Observation.start("test-observation", () -> customContext, observationRegistry).stop();
				assertThat(handlers).hasSize(1);
				assertThat(handlers.get(0)).isInstanceOf(ObservationHandlerWithCustomContext.class);
				assertThat(context).hasSingleBean(DefaultMeterObservationHandler.class);
				assertThat(context).doesNotHaveBean(TracingAwareMeterObservationHandler.class);
			});
	}

	@Test
	void autoConfiguresTracingAwareMeterObservationHandler() {
		this.tracingContextRunner.withUserConfiguration(CustomTracingObservationHandlers.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			List<ObservationHandler<?>> handlers = context.getBean(CalledHandlers.class).getCalledHandlers();
			// Intentionally not stopped since that will trigger additional logic in
			// TracingAwareMeterObservationHandler that we don't test here
			Observation.start("test-observation", observationRegistry);
			assertThat(handlers).hasSize(1);
			assertThat(handlers.get(0)).isInstanceOf(CustomTracingObservationHandler.class);
			assertThat(context).hasSingleBean(TracingAwareMeterObservationHandler.class);
			assertThat(context.getBeansOfType(ObservationHandler.class)).hasSize(2);
		});
	}

	@Test
	void autoConfiguresObservationHandlerWhenTracingIsActive() {
		this.tracingContextRunner.withUserConfiguration(ObservationHandlersTracing.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			List<ObservationHandler<?>> handlers = context.getBean(CalledHandlers.class).getCalledHandlers();
			Observation.start("test-observation", observationRegistry).stop();
			assertThat(handlers).hasSize(3);
			// Multiple TracingObservationHandler are wrapped in
			// FirstMatchingCompositeObservationHandler, which calls only the first one
			assertThat(handlers.get(0)).isInstanceOf(CustomTracingObservationHandler.class);
			assertThat(((CustomTracingObservationHandler) handlers.get(0)).getName())
				.isEqualTo("customTracingHandler1");
			// Multiple MeterObservationHandler are wrapped in
			// FirstMatchingCompositeObservationHandler, which calls only the first one
			assertThat(handlers.get(1)).isInstanceOf(CustomMeterObservationHandler.class);
			assertThat(((CustomMeterObservationHandler) handlers.get(1)).getName())
				.isEqualTo("customMeterObservationHandler1");
			// Regular handlers are registered last
			assertThat(handlers.get(2)).isInstanceOf(CustomObservationHandler.class);
			assertThat(context).doesNotHaveBean(TracingAwareMeterObservationHandler.class);
			assertThat(context).doesNotHaveBean(DefaultMeterObservationHandler.class);
		});
	}

	@Test
	void shouldNotDisableSpringSecurityObservationsByDefault() {
		this.contextRunner.run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("spring.security.filterchains", observationRegistry).stop();
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThat(meterRegistry.get("spring.security.filterchains").timer().count()).isOne();
		});
	}

	@Test
	void shouldDisableSpringSecurityObservationsIfPropertyIsSet() {
		this.contextRunner.withPropertyValues("management.observations.enable.spring.security=false").run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("spring.security.filterchains", observationRegistry).stop();
			MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
			assertThatThrownBy(() -> meterRegistry.get("spring.security.filterchains").timer())
				.isInstanceOf(MeterNotFoundException.class);
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
	static class ObservationFilters {

		@Bean
		@Order(1)
		ObservationFilter observationFilterOne() {
			return (context) -> context.addLowCardinalityKeyValue(KeyValue.of("filter", "one"));
		}

		@Bean
		@Order(0)
		ObservationFilter observationFilterTwo() {
			return (context) -> context.addLowCardinalityKeyValue(KeyValue.of("filter", "two"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomObservedAspectConfiguration {

		@Bean
		ObservedAspect customObservedAspect(ObservationRegistry observationRegistry) {
			return new ObservedAspect(observationRegistry);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomGlobalObservationConvention {

		@Bean
		GlobalObservationConvention<?> customConvention() {
			return new GlobalObservationConvention<>() {
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
	static class TracerConfiguration {

		@Bean
		Tracer tracer() {
			return mock(Tracer.class); // simulating tracer configuration
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(CalledHandlersConfiguration.class)
	static class CustomTracingObservationHandlers {

		@Bean
		CustomTracingObservationHandler customTracingHandler1(CalledHandlers calledHandlers) {
			return new CustomTracingObservationHandler("customTracingHandler1", calledHandlers);
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
