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

import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry propagation configurations. They are imported by
 * {@link OpenTelemetryAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class OpenTelemetryPropagationConfigurations {

	/**
	 * Propagates traces but no baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "management.tracing.baggage", name = "enabled", havingValue = "false")
	@EnableConfigurationProperties(TracingProperties.class)
	static class PropagationWithoutBaggage {

		/**
         * Creates a {@link TextMapPropagator} based on the provided {@link TracingProperties}.
         * 
         * @param properties the {@link TracingProperties} used to configure the propagator
         * @return the created {@link TextMapPropagator}
         */
        @Bean
		@ConditionalOnEnabledTracing
		TextMapPropagator textMapPropagator(TracingProperties properties) {
			return CompositeTextMapPropagator.create(properties.getPropagation(), null);
		}

	}

	/**
	 * Propagates traces and baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "management.tracing.baggage", name = "enabled", matchIfMissing = true)
	@EnableConfigurationProperties(TracingProperties.class)
	static class PropagationWithBaggage {

		private final TracingProperties tracingProperties;

		/**
         * Initializes a new instance of the PropagationWithBaggage class with the specified tracing properties.
         * 
         * @param tracingProperties The tracing properties to be used for propagation.
         */
        PropagationWithBaggage(TracingProperties tracingProperties) {
			this.tracingProperties = tracingProperties;
		}

		/**
         * Creates a TextMapPropagator with baggage based on the provided OtelCurrentTraceContext.
         * 
         * @param otelCurrentTraceContext The OtelCurrentTraceContext used for creating the baggage propagator.
         * @return The created TextMapPropagator with baggage.
         */
        @Bean
		@ConditionalOnEnabledTracing
		TextMapPropagator textMapPropagatorWithBaggage(OtelCurrentTraceContext otelCurrentTraceContext) {
			List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
			List<String> tagFields = this.tracingProperties.getBaggage().getTagFields();
			BaggageTextMapPropagator baggagePropagator = new BaggageTextMapPropagator(remoteFields,
					new OtelBaggageManager(otelCurrentTraceContext, remoteFields, tagFields));
			return CompositeTextMapPropagator.create(this.tracingProperties.getPropagation(), baggagePropagator);
		}

		/**
         * Creates a new instance of Slf4JBaggageEventListener if no other bean of the same type is present in the application context and if the property "management.tracing.baggage.correlation.enabled" is either not present or set to true.
         * 
         * This method is annotated with @Bean, @ConditionalOnMissingBean, and @ConditionalOnProperty annotations to ensure that the bean is only created if the specified conditions are met.
         * 
         * The created Slf4JBaggageEventListener instance is initialized with the baggage correlation fields specified in the tracing properties.
         * 
         * @return a new instance of Slf4JBaggageEventListener
         */
        @Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "management.tracing.baggage.correlation", name = "enabled",
				matchIfMissing = true)
		Slf4JBaggageEventListener otelSlf4JBaggageEventListener() {
			return new Slf4JBaggageEventListener(this.tracingProperties.getBaggage().getCorrelation().getFields());
		}

	}

	/**
	 * Propagates neither traces nor baggage.
	 */
	@Configuration(proxyBeanMethods = false)
	static class NoPropagation {

		/**
         * Returns a TextMapPropagator that does nothing.
         * This method is annotated with @ConditionalOnMissingBean, which means it will only be called if there is no other bean of type TextMapPropagator present in the application context.
         * 
         * @return a TextMapPropagator that does nothing
         */
        @Bean
		@ConditionalOnMissingBean
		TextMapPropagator noopTextMapPropagator() {
			return TextMapPropagator.noop();
		}

	}

}
