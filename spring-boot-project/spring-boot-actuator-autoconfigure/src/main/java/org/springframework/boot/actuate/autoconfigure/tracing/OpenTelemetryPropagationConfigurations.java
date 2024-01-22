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

		PropagationWithBaggage(TracingProperties tracingProperties) {
			this.tracingProperties = tracingProperties;
		}

		@Bean
		@ConditionalOnEnabledTracing
		TextMapPropagator textMapPropagatorWithBaggage(OtelCurrentTraceContext otelCurrentTraceContext) {
			List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
			List<String> tagFields = this.tracingProperties.getBaggage().getTagFields();
			BaggageTextMapPropagator baggagePropagator = new BaggageTextMapPropagator(remoteFields,
					new OtelBaggageManager(otelCurrentTraceContext, remoteFields, tagFields));
			return CompositeTextMapPropagator.create(this.tracingProperties.getPropagation(), baggagePropagator);
		}

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

		@Bean
		@ConditionalOnMissingBean
		TextMapPropagator noopTextMapPropagator() {
			return TextMapPropagator.noop();
		}

	}

}
