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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.Collections;
import java.util.List;

import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelSpanCustomizer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher;
import io.micrometer.tracing.otel.bridge.Slf4JBaggageEventListener;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@AutoConfiguration(before = MicrometerTracingAutoConfiguration.class)
@ConditionalOnEnabledTracing
@ConditionalOnClass({ OtelTracer.class, SdkTracerProvider.class, OpenTelemetry.class })
@EnableConfigurationProperties(TracingProperties.class)
public class OpenTelemetryAutoConfiguration {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "application";

	private final TracingProperties tracingProperties;

	OpenTelemetryAutoConfiguration(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider, ContextPropagators contextPropagators) {
		return OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).setPropagators(contextPropagators)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	SdkTracerProvider otelSdkTracerProvider(Environment environment, ObjectProvider<SpanProcessor> spanProcessors,
			Sampler sampler) {
		String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
		SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setSampler(sampler)
				.setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)));
		spanProcessors.orderedStream().forEach(builder::addSpanProcessor);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	ContextPropagators otelContextPropagators(ObjectProvider<TextMapPropagator> textMapPropagators) {
		return ContextPropagators.create(TextMapPropagator.composite(textMapPropagators.orderedStream().toList()));
	}

	@Bean
	@ConditionalOnMissingBean
	Sampler otelSampler() {
		return Sampler.traceIdRatioBased(this.tracingProperties.getSampling().getProbability());
	}

	@Bean
	SpanProcessor otelSpanProcessor(ObjectProvider<SpanExporter> spanExporters) {
		return SpanProcessor.composite(spanExporters.orderedStream().map(this::buildBatchSpanProcessor).toList());
	}

	private SpanProcessor buildBatchSpanProcessor(SpanExporter exporter) {
		return BatchSpanProcessor.builder(exporter).build();
	}

	@Bean
	@ConditionalOnMissingBean
	Tracer otelTracer(OpenTelemetry openTelemetry) {
		return openTelemetry.getTracer("org.springframework.boot", SpringBootVersion.getVersion());
	}

	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
	OtelTracer micrometerOtelTracer(Tracer tracer, EventPublisher eventPublisher,
			OtelCurrentTraceContext otelCurrentTraceContext) {
		return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
				new OtelBaggageManager(otelCurrentTraceContext, this.tracingProperties.getBaggage().getRemoteFields(),
						Collections.emptyList()));
	}

	@Bean
	@ConditionalOnMissingBean
	OtelPropagator otelPropagator(ContextPropagators contextPropagators, Tracer tracer) {
		return new OtelPropagator(contextPropagators, tracer);
	}

	@Bean
	@ConditionalOnMissingBean
	EventPublisher otelTracerEventPublisher(List<EventListener> eventListeners) {
		return new OTelEventPublisher(eventListeners);
	}

	@Bean
	@ConditionalOnMissingBean
	OtelCurrentTraceContext otelCurrentTraceContext(EventPublisher publisher) {
		ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
		return new OtelCurrentTraceContext();
	}

	@Bean
	@ConditionalOnMissingBean
	Slf4JEventListener otelSlf4JEventListener() {
		return new Slf4JEventListener();
	}

	@Bean
	@ConditionalOnMissingBean(SpanCustomizer.class)
	OtelSpanCustomizer otelSpanCustomizer() {
		return new OtelSpanCustomizer();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "management.tracing.baggage", name = "enabled", matchIfMissing = true)
	static class BaggageConfiguration {

		private final TracingProperties tracingProperties;

		BaggageConfiguration(TracingProperties tracingProperties) {
			this.tracingProperties = tracingProperties;
		}

		@Bean
		@ConditionalOnProperty(prefix = "management.tracing.propagation", name = "type", havingValue = "W3C",
				matchIfMissing = true)
		TextMapPropagator w3cTextMapPropagatorWithBaggage(OtelCurrentTraceContext otelCurrentTraceContext) {
			List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
			return TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(),
					W3CBaggagePropagator.getInstance(), new BaggageTextMapPropagator(remoteFields,
							new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
		}

		@Bean
		@ConditionalOnProperty(prefix = "management.tracing.propagation", name = "type", havingValue = "B3")
		TextMapPropagator b3BaggageTextMapPropagator(OtelCurrentTraceContext otelCurrentTraceContext) {
			List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
			return TextMapPropagator.composite(B3Propagator.injectingSingleHeader(),
					new BaggageTextMapPropagator(remoteFields,
							new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList())));
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "management.tracing.baggage.correlation", name = "enabled",
				matchIfMissing = true)
		Slf4JBaggageEventListener otelSlf4JBaggageEventListener() {
			return new Slf4JBaggageEventListener(this.tracingProperties.getBaggage().getCorrelation().getFields());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "management.tracing.baggage", name = "enabled", havingValue = "false")
	static class NoBaggageConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "management.tracing.propagation", name = "type", havingValue = "B3")
		B3Propagator b3TextMapPropagator() {
			return B3Propagator.injectingSingleHeader();
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "management.tracing.propagation", name = "type", havingValue = "W3C",
				matchIfMissing = true)
		W3CTraceContextPropagator w3cTextMapPropagatorWithoutBaggage() {
			return W3CTraceContextPropagator.getInstance();
		}

	}

	static class OTelEventPublisher implements EventPublisher {

		private final List<EventListener> listeners;

		OTelEventPublisher(List<EventListener> listeners) {
			this.listeners = listeners;
		}

		@Override
		public void publishEvent(Object event) {
			for (EventListener listener : this.listeners) {
				listener.onEvent(event);
			}
		}

	}

}
