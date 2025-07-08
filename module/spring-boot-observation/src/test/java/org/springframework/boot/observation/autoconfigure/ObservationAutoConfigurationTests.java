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

package org.springframework.boot.observation.autoconfigure;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.common.annotation.ValueExpressionResolver;
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
import org.aspectj.weaver.Advice;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ObservationAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Jonatan Ivanov
 * @author Vedran Pavic
 */
class ObservationAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("management.observations.annotations.enabled=true")
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class))
		.withUserConfiguration(ObservationHandlers.class);

	@Test
	void beansShouldNotBeSuppliedWhenMicrometerObservationIsNotOnClassPath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.observation")).run((context) -> {
			assertThat(context).doesNotHaveBean(ObservationRegistry.class);
			assertThat(context).doesNotHaveBean(ObservedAspect.class);
		});
	}

	@Test
	void supplyObservationRegistry() {
		this.contextRunner.run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test-observation", observationRegistry).stop();
			assertThat(context).hasSingleBean(ObservedAspect.class);
		});
	}

	@Test
	void allowsObservedAspectToBeDisabled() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Advice.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ObservedAspect.class));
	}

	@Test
	void allowsObservedAspectToBeDisabledWithProperty() {
		this.contextRunner.withPropertyValues("management.observations.annotations.enabled=false")
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
			Observation observation = Observation.start("observation1", observationRegistry);
			assertThat(observation).isNotEqualTo(Observation.NOOP);
			observation.stop();
			// This isn't allowed by ObservationPredicates.customPredicate
			observation = Observation.start("observation2", observationRegistry);
			assertThat(observation).isEqualTo(Observation.NOOP);
			observation.stop();
		});
	}

	@Test
	void autoConfiguresObservationFilters() {
		this.contextRunner.withUserConfiguration(ObservationFilters.class).run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation observation = Observation.start("filtered", observationRegistry);
			observation.stop();
			observation.getContext().getLowCardinalityKeyValues().forEach((kv) -> System.out.println(kv.getKey()));
			assertThat(observation.getContext().getLowCardinalityKeyValue("filter").getValue()).isEqualTo("one");
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
			Observation observation = Observation.start("keyvalues", observationRegistry);
			observation.stop();
			assertThat(observation.getContext().getLowCardinalityKeyValue("a").getValue()).isEqualTo("alpha");
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
	void shouldNotDisableSpringSecurityObservationsByDefault() {
		this.contextRunner.run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation observation = Observation.start("spring.security.filterchains", observationRegistry);
			assertThat(observation).isNotEqualTo(Observation.NOOP);
			observation.stop();
		});
	}

	@Test
	void shouldDisableSpringSecurityObservationsIfPropertyIsSet() {
		this.contextRunner.withPropertyValues("management.observations.enable.spring.security=false").run((context) -> {
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation observation = Observation.start("spring.security.filterchains", observationRegistry);
			assertThat(observation).isEqualTo(Observation.NOOP);
			observation.stop();
		});
	}

	@Test
	void autoConfiguresValueExpressionResolver() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(SpelValueExpressionResolver.class));
	}

	@Test
	void allowsUserDefinedValueExpressionResolver() {
		this.contextRunner.withBean(ValueExpressionResolver.class, () -> mock(ValueExpressionResolver.class))
			.run((context) -> assertThat(context).hasSingleBean(ValueExpressionResolver.class)
				.doesNotHaveBean(SpelValueExpressionResolver.class));
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
		ObservationHandler<Context> customObservationHandler() {
			return new CustomObservationHandler();
		}

	}

	private static final class CustomObservationHandler implements ObservationHandler<Context> {

		@Override
		public boolean supportsContext(Context context) {
			return true;
		}

	}

}
