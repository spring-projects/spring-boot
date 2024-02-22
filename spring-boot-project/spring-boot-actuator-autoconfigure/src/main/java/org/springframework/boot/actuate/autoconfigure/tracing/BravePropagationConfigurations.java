/*
 * Copyright 2012-2024 the original author or authors.
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

import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagation.FactoryBuilder;
import brave.baggage.BaggagePropagationConfig;
import brave.baggage.BaggagePropagationCustomizer;
import brave.baggage.CorrelationScopeConfig.SingleCorrelationField;
import brave.baggage.CorrelationScopeCustomizer;
import brave.baggage.CorrelationScopeDecorator;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext.ScopeDecorator;
import brave.propagation.Propagation;
import brave.propagation.Propagation.Factory;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Baggage.Correlation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Brave propagation configurations. They are imported by {@link BraveAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class BravePropagationConfigurations {

	/**
	 * Propagates traces but no baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "management.tracing.baggage.enabled", havingValue = "false")
	static class PropagationWithoutBaggage {

		@Bean
		@ConditionalOnMissingBean(Factory.class)
		@ConditionalOnEnabledTracing
		CompositePropagationFactory propagationFactory(TracingProperties properties) {
			return CompositePropagationFactory.create(properties.getPropagation());
		}

	}

	/**
	 * Propagates traces and baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(value = "management.tracing.baggage.enabled", matchIfMissing = true)
	@EnableConfigurationProperties(TracingProperties.class)
	static class PropagationWithBaggage {

		private final TracingProperties tracingProperties;

		PropagationWithBaggage(TracingProperties tracingProperties) {
			this.tracingProperties = tracingProperties;
		}

		@Bean
		@ConditionalOnMissingBean
		BaggagePropagation.FactoryBuilder propagationFactoryBuilder(
				ObjectProvider<BaggagePropagationCustomizer> baggagePropagationCustomizers) {
			// There's a chicken-and-egg problem here: to create a builder, we need a
			// factory. But the CompositePropagationFactory needs data from the builder.
			// We create a throw-away builder with a throw-away factory, and then copy the
			// config to the real builder.
			FactoryBuilder throwAwayBuilder = BaggagePropagation.newFactoryBuilder(createThrowAwayFactory());
			baggagePropagationCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(throwAwayBuilder));
			CompositePropagationFactory propagationFactory = CompositePropagationFactory.create(
					this.tracingProperties.getPropagation(),
					new BraveBaggageManager(this.tracingProperties.getBaggage().getTagFields()),
					LocalBaggageFields.extractFrom(throwAwayBuilder));
			FactoryBuilder builder = BaggagePropagation.newFactoryBuilder(propagationFactory);
			throwAwayBuilder.configs().forEach(builder::add);
			return builder;
		}

		private Factory createThrowAwayFactory() {
			return new Factory() {

				@Override
				public Propagation<String> get() {
					return null;
				}

			};
		}

		@Bean
		BaggagePropagationCustomizer remoteFieldsBaggagePropagationCustomizer() {
			return (builder) -> {
				List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
				for (String fieldName : remoteFields) {
					builder.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create(fieldName)));
				}
				List<String> localFields = this.tracingProperties.getBaggage().getLocalFields();
				for (String localFieldName : localFields) {
					builder.add(BaggagePropagationConfig.SingleBaggageField.local(BaggageField.create(localFieldName)));
				}
			};
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledTracing
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

	/**
	 * Propagates neither traces nor baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	static class NoPropagation {

		@Bean
		@ConditionalOnMissingBean(Factory.class)
		CompositePropagationFactory noopPropagationFactory() {
			return CompositePropagationFactory.noop();
		}

	}

}
