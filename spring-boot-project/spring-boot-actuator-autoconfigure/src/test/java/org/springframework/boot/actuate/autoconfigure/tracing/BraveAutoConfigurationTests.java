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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import brave.Span;
import brave.SpanCustomizer;
import brave.Tracer;
import brave.Tracing;
import brave.baggage.BaggagePropagation;
import brave.baggage.CorrelationScopeConfig.SingleCorrelationField;
import brave.handler.SpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.ScopeDecorator;
import brave.propagation.Propagation;
import brave.propagation.Propagation.Factory;
import brave.propagation.TraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveSpanCustomizer;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.CompositeSpanHandler;
import io.micrometer.tracing.brave.bridge.W3CPropagation;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfigurationTests.SpanHandlerConfiguration.AdditionalSpanHandler;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.IncompatibleConfigurationException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BraveAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Jonatan Ivanov
 */
class BraveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class));

	@Test
	void shouldSupplyDefaultBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BraveAutoConfiguration.class);
			assertThat(context).hasSingleBean(Tracing.class);
			assertThat(context).hasSingleBean(Tracer.class);
			assertThat(context).hasSingleBean(CurrentTraceContext.class);
			assertThat(context).hasSingleBean(Factory.class);
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasSingleBean(BraveTracer.class);
			assertThat(context).hasSingleBean(Propagation.Factory.class);
			assertThat(context).hasSingleBean(BaggagePropagation.FactoryBuilder.class);
			assertThat(context).hasSingleBean(BraveTracer.class);
			assertThat(context).hasSingleBean(CompositeSpanHandler.class);
			assertThat(context).hasSingleBean(SpanCustomizer.class);
			assertThat(context).hasSingleBean(BraveSpanCustomizer.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
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
			assertThat(context).hasBean("customMicrometerTracer");
			assertThat(context).hasSingleBean(io.micrometer.tracing.Tracer.class);
			assertThat(context).hasBean("customBraveBaggageManager");
			assertThat(context).hasSingleBean(BraveBaggageManager.class);
			assertThat(context).hasBean("customCompositeSpanHandler");
			assertThat(context).hasSingleBean(CompositeSpanHandler.class);
			assertThat(context).hasBean("customSpanCustomizer");
			assertThat(context).hasSingleBean(SpanCustomizer.class);
			assertThat(context).hasBean("customMicrometerSpanCustomizer");
			assertThat(context).hasSingleBean(io.micrometer.tracing.SpanCustomizer.class);
		});
	}

	@Test
	void shouldSupplyMicrometerBeans() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(BraveTracer.class));
	}

	@Test
	void shouldNotSupplyBeansIfBraveIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("brave"))
			.run((context) -> assertThat(context).doesNotHaveBean(BraveAutoConfiguration.class));
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer"))
			.run((context) -> assertThat(context).doesNotHaveBean(BraveAutoConfiguration.class));
	}

	@Test
	void shouldSupplyW3CPropagationFactoryByDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasBean("propagationFactory");
			Factory factory = context.getBean(Factory.class);
			Stream<Class<?>> injectors = getInjectors(factory).stream().map(Object::getClass);
			assertThat(injectors).containsExactly(W3CPropagation.class);
			assertThat(context).hasSingleBean(BaggagePropagation.FactoryBuilder.class);
		});
	}

	@Test
	void shouldSupplyB3PropagationFactoryViaProperty() {
		this.contextRunner.withPropertyValues("management.tracing.propagation.type=B3").run((context) -> {
			assertThat(context).hasBean("propagationFactory");
			Factory factory = context.getBean(Factory.class);
			List<Factory> injectors = getInjectors(factory);
			assertThat(injectors).extracting(Factory::toString).containsExactly("B3Propagation");
			assertThat(context).hasSingleBean(BaggagePropagation.FactoryBuilder.class);
		});
	}

	@Test
	void shouldUseB3SingleWithParentWhenPropagationTypeIsB3() {
		this.contextRunner
			.withPropertyValues("management.tracing.propagation.type=B3", "management.tracing.sampling.probability=1.0")
			.run((context) -> {
				Propagation<String> propagation = context.getBean(Factory.class).get();
				Tracer tracer = context.getBean(Tracing.class).tracer();
				Span child;
				Span parent = tracer.nextSpan().name("parent");
				try (Tracer.SpanInScope ignored = tracer.withSpanInScope(parent.start())) {
					child = tracer.nextSpan().name("child");
					child.start().finish();
				}
				finally {
					parent.finish();
				}

				Map<String, String> map = new HashMap<>();
				TraceContext childContext = child.context();
				propagation.injector(this::injectToMap).inject(childContext, map);
				assertThat(map).containsExactly(Map.entry("b3", "%s-%s-1-%s".formatted(childContext.traceIdString(),
						childContext.spanIdString(), childContext.parentIdString())));
			});
	}

	@Test
	void shouldNotSupplyCorrelationScopeDecoratorIfBaggageDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.baggage.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean("correlationScopeDecorator"));
	}

	@Test
	void shouldSupplyW3CWithoutBaggageByDefaultIfBaggageDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.baggage.enabled=false").run((context) -> {
			assertThat(context).hasBean("propagationFactory");
			Factory factory = context.getBean(Factory.class);
			Stream<Class<?>> injectors = getInjectors(factory).stream().map(Object::getClass);
			assertThat(injectors).containsExactly(W3CPropagation.class);
			assertThat(context).doesNotHaveBean(BaggagePropagation.FactoryBuilder.class);
		});
	}

	@Test
	void shouldSupplyB3WithoutBaggageIfBaggageDisabledAndB3Picked() {
		this.contextRunner
			.withPropertyValues("management.tracing.baggage.enabled=false", "management.tracing.propagation.type=B3")
			.run((context) -> {
				assertThat(context).hasBean("propagationFactory");
				Factory factory = context.getBean(Factory.class);
				List<Factory> injectors = getInjectors(factory);
				assertThat(injectors).extracting(Factory::toString).containsExactly("B3Propagation");
				assertThat(context).doesNotHaveBean(BaggagePropagation.FactoryBuilder.class);
			});
	}

	@Test
	void shouldNotApplyCorrelationFieldsIfBaggageCorrelationDisabled() {
		this.contextRunner
			.withPropertyValues("management.tracing.baggage.correlation.enabled=false",
					"management.tracing.baggage.correlation.fields=alpha,bravo")
			.run((context) -> {
				ScopeDecorator scopeDecorator = context.getBean(ScopeDecorator.class);
				assertThat(scopeDecorator)
					.extracting("fields", InstanceOfAssertFactories.array(SingleCorrelationField[].class))
					.hasSize(2);
			});
	}

	@Test
	void shouldApplyCorrelationFieldsIfBaggageCorrelationEnabled() {
		this.contextRunner
			.withPropertyValues("management.tracing.baggage.correlation.enabled=true",
					"management.tracing.baggage.correlation.fields=alpha,bravo")
			.run((context) -> {
				ScopeDecorator scopeDecorator = context.getBean(ScopeDecorator.class);
				assertThat(scopeDecorator)
					.extracting("fields", InstanceOfAssertFactories.array(SingleCorrelationField[].class))
					.hasSize(4);
			});
	}

	@Test
	void shouldSupplyMdcCorrelationScopeDecoratorIfBaggageCorrelationDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.baggage.correlation.enabled=false")
			.run((context) -> assertThat(context).hasBean("mdcCorrelationScopeDecoratorBuilder"));
	}

	@Test
	void shouldHave128BitTraceId() {
		this.contextRunner.run((context) -> {
			Tracing tracing = context.getBean(Tracing.class);
			Span span = tracing.tracer().nextSpan();
			assertThat(span.context().traceIdString()).hasSize(32);
		});
	}

	@Test
	void shouldNotSupportJoinedSpansByDefault() {
		this.contextRunner.run((context) -> {
			Tracing tracing = context.getBean(Tracing.class);
			Span parentSpan = tracing.tracer().nextSpan();
			Span childSpan = tracing.tracer().joinSpan(parentSpan.context());
			assertThat(childSpan.context().traceIdString()).isEqualTo(parentSpan.context().traceIdString());
			assertThat(childSpan.context().spanIdString()).isNotEqualTo(parentSpan.context().spanIdString());
			assertThat(childSpan.context().parentIdString()).isEqualTo(parentSpan.context().spanIdString());
			assertThat(parentSpan.context().parentIdString()).isNull();
		});
	}

	@Test
	void shouldSupportJoinedSpansIfB3UsedAndBackendSupportsIt() {
		this.contextRunner
			.withPropertyValues("management.tracing.propagation.type=B3",
					"management.tracing.brave.span-joining-supported=true")
			.run((context) -> {
				Tracing tracing = context.getBean(Tracing.class);
				Span parentSpan = tracing.tracer().nextSpan();
				Span childSpan = tracing.tracer().joinSpan(parentSpan.context());
				assertThat(childSpan.context().traceIdString()).isEqualTo(parentSpan.context().traceIdString());
				assertThat(childSpan.context().spanIdString()).isEqualTo(parentSpan.context().spanIdString());
				assertThat(childSpan.context().parentIdString()).isNull();
				assertThat(parentSpan.context().parentIdString()).isNull();
			});
	}

	@Test
	void shouldFailIfSupportJoinedSpansIsEnabledAndW3cIsChosenAsType() {
		this.contextRunner
			.withPropertyValues("management.tracing.propagation.type=W3C",
					"management.tracing.brave.span-joining-supported=true")
			.run((context) -> assertThatThrownBy(() -> context.getBean(Tracing.class)).rootCause()
				.isExactlyInstanceOf(IncompatibleConfigurationException.class)
				.hasMessage(
						"The following configuration properties have incompatible values: [management.tracing.propagation.type, management.tracing.brave.span-joining-supported]"));
	}

	@Test
	void shouldFailIfSupportJoinedSpansIsEnabledAndW3cIsChosenAsConsume() {
		this.contextRunner.withPropertyValues("management.tracing.propagation.produce=B3",
				"management.tracing.propagation.consume=W3C", "management.tracing.brave.span-joining-supported=true")
			.run((context) -> assertThatThrownBy(() -> context.getBean(Tracing.class)).rootCause()
				.isExactlyInstanceOf(IncompatibleConfigurationException.class)
				.hasMessage(
						"The following configuration properties have incompatible values: [management.tracing.propagation.consume, management.tracing.brave.span-joining-supported]"));
	}

	@Test
	void shouldFailIfSupportJoinedSpansIsEnabledAndW3cIsChosenAsProduce() {
		this.contextRunner.withPropertyValues("management.tracing.propagation.consume=B3",
				"management.tracing.propagation.produce=W3C", "management.tracing.brave.span-joining-supported=true")
			.run((context) -> assertThatThrownBy(() -> context.getBean(Tracing.class)).rootCause()
				.isExactlyInstanceOf(IncompatibleConfigurationException.class)
				.hasMessage(
						"The following configuration properties have incompatible values: [management.tracing.propagation.produce, management.tracing.brave.span-joining-supported]"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	void compositeSpanHandlerShouldBeFirstSpanHandler() {
		this.contextRunner.withUserConfiguration(SpanHandlerConfiguration.class).run((context) -> {
			Tracing tracing = context.getBean(Tracing.class);
			assertThat(tracing).extracting("tracer.spanHandler.delegate.handlers")
				.asInstanceOf(InstanceOfAssertFactories.array(SpanHandler[].class))
				.extracting((handler) -> (Class) handler.getClass())
				.containsExactly(CompositeSpanHandler.class, AdditionalSpanHandler.class);
		});
	}

	@Test
	void compositeSpanHandlerUsesFilterPredicateAndReportersInOrder() {
		this.contextRunner.withUserConfiguration(CompositeSpanHandlerComponentsConfiguration.class).run((context) -> {
			CompositeSpanHandlerComponentsConfiguration components = context
				.getBean(CompositeSpanHandlerComponentsConfiguration.class);
			CompositeSpanHandler composite = context.getBean(CompositeSpanHandler.class);
			assertThat(composite).extracting("spanFilters")
				.asList()
				.containsExactly(components.filter1, components.filter2);
			assertThat(composite).extracting("filters")
				.asList()
				.containsExactly(components.predicate2, components.predicate1);
			assertThat(composite).extracting("reporters")
				.asList()
				.containsExactly(components.reporter1, components.reporter3, components.reporter2);
		});
	}

	private void injectToMap(Map<String, String> map, String key, String value) {
		map.put(key, value);
	}

	private List<Factory> getInjectors(Factory factory) {
		assertThat(factory).as("factory").isNotNull();
		if (factory instanceof CompositePropagationFactory compositePropagationFactory) {
			return compositePropagationFactory.getInjectors().toList();
		}
		Assertions.fail("Expected CompositePropagationFactory, found %s".formatted(factory.getClass()));
		throw new AssertionError("Unreachable");
	}

	@Configuration(proxyBeanMethods = false)
	static class CompositeSpanHandlerComponentsConfiguration {

		private final SpanFilter filter1 = mock(SpanFilter.class);

		private final SpanFilter filter2 = mock(SpanFilter.class);

		private final SpanExportingPredicate predicate1 = mock(SpanExportingPredicate.class);

		private final SpanExportingPredicate predicate2 = mock(SpanExportingPredicate.class);

		private final SpanReporter reporter1 = mock(SpanReporter.class);

		private final SpanReporter reporter2 = mock(SpanReporter.class);

		private final SpanReporter reporter3 = mock(SpanReporter.class);

		@Bean
		@Order(1)
		SpanFilter filter1() {
			return this.filter1;
		}

		@Bean
		@Order(2)
		SpanFilter filter2() {
			return this.filter2;
		}

		@Bean
		@Order(2)
		SpanExportingPredicate predicate1() {
			return this.predicate1;
		}

		@Bean
		@Order(1)
		SpanExportingPredicate predicate2() {
			return this.predicate2;
		}

		@Bean
		@Order(1)
		SpanReporter reporter1() {
			return this.reporter1;
		}

		@Bean
		@Order(3)
		SpanReporter reporter2() {
			return this.reporter2;
		}

		@Bean
		@Order(2)
		SpanReporter reporter3() {
			return this.reporter3;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SpanHandlerConfiguration {

		@Bean
		SpanHandler additionalSpanHandler() {
			return new AdditionalSpanHandler();
		}

		static class AdditionalSpanHandler extends SpanHandler {

		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		Tracing customTracing() {
			return mock(Tracing.class);
		}

		@Bean
		Tracer customTracer() {
			return mock(Tracer.class);
		}

		@Bean
		CurrentTraceContext customCurrentTraceContext() {
			return mock(CurrentTraceContext.class);
		}

		@Bean
		Factory customFactory() {
			return mock(Factory.class);
		}

		@Bean
		Sampler customSampler() {
			return mock(Sampler.class);
		}

		@Bean
		io.micrometer.tracing.Tracer customMicrometerTracer() {
			return mock(io.micrometer.tracing.Tracer.class);
		}

		@Bean
		BraveBaggageManager customBraveBaggageManager() {
			return mock(BraveBaggageManager.class);
		}

		@Bean
		CompositeSpanHandler customCompositeSpanHandler() {
			return new CompositeSpanHandler(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		}

		@Bean
		SpanCustomizer customSpanCustomizer() {
			return mock(SpanCustomizer.class);
		}

		@Bean
		io.micrometer.tracing.SpanCustomizer customMicrometerSpanCustomizer() {
			return mock(io.micrometer.tracing.SpanCustomizer.class);
		}

	}

}
