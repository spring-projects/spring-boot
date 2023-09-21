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

import brave.CurrentSpanCustomizer;
import brave.SpanCustomizer;
import brave.Tracer;
import brave.Tracing;
import brave.Tracing.Builder;
import brave.TracingCustomizer;
import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagation.FactoryBuilder;
import brave.baggage.BaggagePropagationConfig;
import brave.baggage.BaggagePropagationCustomizer;
import brave.baggage.CorrelationScopeConfig.SingleCorrelationField;
import brave.baggage.CorrelationScopeCustomizer;
import brave.baggage.CorrelationScopeDecorator;
import brave.context.slf4j.MDCScopeDecorator;
import brave.handler.SpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.ScopeDecorator;
import brave.propagation.CurrentTraceContextCustomizer;
import brave.propagation.Propagation;
import brave.propagation.Propagation.Factory;
import brave.propagation.Propagation.KeyFactory;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveSpanCustomizer;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.CompositeSpanHandler;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Baggage.Correlation;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.IncompatibleConfigurationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Brave.
 *
 * @author Moritz Halbritter
 * @author Marcin Grzejszczak
 * @author Jonatan Ivanov
 * @since 3.0.0
 */
@AutoConfiguration(before = MicrometerTracingAutoConfiguration.class)
@ConditionalOnClass({ Tracer.class, BraveTracer.class })
@EnableConfigurationProperties(TracingProperties.class)
public class BraveAutoConfiguration {

	private static final BraveBaggageManager BRAVE_BAGGAGE_MANAGER = new BraveBaggageManager();

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "application";

	@Bean
	@ConditionalOnMissingBean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	CompositeSpanHandler compositeSpanHandler(ObjectProvider<SpanExportingPredicate> predicates,
			ObjectProvider<SpanReporter> reporters, ObjectProvider<SpanFilter> filters) {
		return new CompositeSpanHandler(predicates.orderedStream().toList(), reporters.orderedStream().toList(),
				filters.orderedStream().toList());
	}

	@Bean
	@ConditionalOnMissingBean
	public Tracing braveTracing(Environment environment, TracingProperties properties, List<SpanHandler> spanHandlers,
			List<TracingCustomizer> tracingCustomizers, CurrentTraceContext currentTraceContext,
			Factory propagationFactory, Sampler sampler) {
		if (properties.getBrave().isSpanJoiningSupported()) {
			if (properties.getPropagation().getType() != null
					&& properties.getPropagation().getType().contains(PropagationType.W3C)) {
				throw new IncompatibleConfigurationException("management.tracing.propagation.type",
						"management.tracing.brave.span-joining-supported");
			}
			if (properties.getPropagation().getType() == null
					&& properties.getPropagation().getProduce().contains(PropagationType.W3C)) {
				throw new IncompatibleConfigurationException("management.tracing.propagation.produce",
						"management.tracing.brave.span-joining-supported");
			}
			if (properties.getPropagation().getType() == null
					&& properties.getPropagation().getConsume().contains(PropagationType.W3C)) {
				throw new IncompatibleConfigurationException("management.tracing.propagation.consume",
						"management.tracing.brave.span-joining-supported");
			}
		}
		String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
		Builder builder = Tracing.newBuilder()
			.currentTraceContext(currentTraceContext)
			.traceId128Bit(true)
			.supportsJoin(properties.getBrave().isSpanJoiningSupported())
			.propagationFactory(propagationFactory)
			.sampler(sampler)
			.localServiceName(applicationName);
		spanHandlers.forEach(builder::addSpanHandler);
		for (TracingCustomizer tracingCustomizer : tracingCustomizers) {
			tracingCustomizer.customize(builder);
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public brave.Tracer braveTracer(Tracing tracing) {
		return tracing.tracer();
	}

	@Bean
	@ConditionalOnMissingBean
	public CurrentTraceContext braveCurrentTraceContext(List<CurrentTraceContext.ScopeDecorator> scopeDecorators,
			List<CurrentTraceContextCustomizer> currentTraceContextCustomizers) {
		ThreadLocalCurrentTraceContext.Builder builder = ThreadLocalCurrentTraceContext.newBuilder();
		scopeDecorators.forEach(builder::addScopeDecorator);
		for (CurrentTraceContextCustomizer currentTraceContextCustomizer : currentTraceContextCustomizers) {
			currentTraceContextCustomizer.customize(builder);
		}
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Sampler braveSampler(TracingProperties properties) {
		return Sampler.create(properties.getSampling().getProbability());
	}

	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
	BraveTracer braveTracerBridge(brave.Tracer tracer, CurrentTraceContext currentTraceContext) {
		return new BraveTracer(tracer, new BraveCurrentTraceContext(currentTraceContext), BRAVE_BAGGAGE_MANAGER);
	}

	@Bean
	@ConditionalOnMissingBean
	BravePropagator bravePropagator(Tracing tracing) {
		return new BravePropagator(tracing);
	}

	@Bean
	@ConditionalOnMissingBean(SpanCustomizer.class)
	CurrentSpanCustomizer currentSpanCustomizer(Tracing tracing) {
		return CurrentSpanCustomizer.create(tracing);
	}

	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.SpanCustomizer.class)
	BraveSpanCustomizer braveSpanCustomizer(SpanCustomizer spanCustomizer) {
		return new BraveSpanCustomizer(spanCustomizer);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "management.tracing.baggage.enabled", havingValue = "false")
	static class BraveNoBaggageConfiguration {

		@Bean
		@ConditionalOnMissingBean
		Factory propagationFactory(TracingProperties properties) {
			return CompositePropagationFactory.create(properties.getPropagation());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "management.tracing.baggage.enabled", matchIfMissing = true)
	static class BraveBaggageConfiguration {

		private final TracingProperties tracingProperties;

		BraveBaggageConfiguration(TracingProperties tracingProperties) {
			this.tracingProperties = tracingProperties;
		}

		@Bean
		@ConditionalOnMissingBean
		BaggagePropagation.FactoryBuilder propagationFactoryBuilder(
				ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
			// There's a chicken-and-egg problem here: to create a builder, we need a
			// factory. But the CompositePropagationFactory needs data from the builder.
			// We create a throw-away builder with a throw-away factory, and then copy the
			// config to the real builder
			FactoryBuilder throwAwayBuilder = BaggagePropagation.newFactoryBuilder(createThrowAwayFactory());
			baggagePropagationCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(throwAwayBuilder));
			CompositePropagationFactory propagationFactory = CompositePropagationFactory.create(
					this.tracingProperties.getPropagation(), BRAVE_BAGGAGE_MANAGER,
					LocalBaggageFields.extractFrom(throwAwayBuilder));
			FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(propagationFactory);
			throwAwayBuilder.configs().forEach(builder::add);
			return builder;
		}

		@SuppressWarnings("deprecation")
		private Factory createThrowAwayFactory() {
			return new Factory() {

				@Override
				public <K> Propagation<K> create(KeyFactory<K> keyFactory) {
					return null;
				}

			};
		}

		@Bean
		@Order(0)
		BaggagePropagationCustomizer remoteFieldsBaggagePropagationCustomizer() {
			return (builder) -> {
				List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
				for (String fieldName : remoteFields) {
					builder.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create(fieldName)));
				}
			};
		}

		@Bean
		@ConditionalOnMissingBean
		Factory propagationFactory(BaggagePropagation.FactoryBuilder factoryBuilder) {
			return factoryBuilder.build();
		}

		@Bean
		@ConditionalOnMissingBean
		CorrelationScopeDecorator.Builder mdcCorrelationScopeDecoratorBuilder(
				ObjectProvider<CorrelationScopeCustomizer> correlationScopeCustomizers) {
			CorrelationScopeDecorator.Builder builder = MDCScopeDecorator.newBuilder();
			correlationScopeCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		}

		@Bean
		@Order(0)
		@ConditionalOnProperty(prefix = "management.tracing.baggage.correlation", name = "enabled",
				matchIfMissing = true)
		CorrelationScopeCustomizer correlationFieldsCorrelationScopeCustomizer() {
			return (builder) -> {
				Correlation correlationProperties = this.tracingProperties.getBaggage().getCorrelation();
				for (String field : correlationProperties.getFields()) {
					BaggageField baggageField = BaggageField.create(field);
					SingleCorrelationField correlationField = SingleCorrelationField.newBuilder(baggageField)
						.flushOnUpdate()
						.build();
					builder.add(correlationField);
				}
			};
		}

		@Bean
		@ConditionalOnMissingBean(CorrelationScopeDecorator.class)
		ScopeDecorator correlationScopeDecorator(CorrelationScopeDecorator.Builder builder) {
			return builder.build();
		}

	}

}
