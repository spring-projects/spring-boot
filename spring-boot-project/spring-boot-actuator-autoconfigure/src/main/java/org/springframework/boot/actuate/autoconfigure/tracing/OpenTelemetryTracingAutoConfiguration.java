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

import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.exporter.SpanFilter;
import io.micrometer.tracing.exporter.SpanReporter;
import io.micrometer.tracing.otel.bridge.CompositeSpanExporter;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelSpanCustomizer;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.otel.bridge.OtelTracer.EventPublisher;
import io.micrometer.tracing.otel.bridge.Slf4JEventListener;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenTelemetry tracing.
 *
 * @author Moritz Halbritter
 * @author Marcin Grzejszczak
 * @author Yanming Zhou
 * @since 3.4.0
 */
@AutoConfiguration(before = { MicrometerTracingAutoConfiguration.class, NoopTracerAutoConfiguration.class })
@ConditionalOnClass({ OtelTracer.class, SdkTracerProvider.class, OpenTelemetry.class })
@EnableConfigurationProperties(TracingProperties.class)
@Import({ OpenTelemetryPropagationConfigurations.PropagationWithoutBaggage.class,
		OpenTelemetryPropagationConfigurations.PropagationWithBaggage.class,
		OpenTelemetryPropagationConfigurations.NoPropagation.class })
public class OpenTelemetryTracingAutoConfiguration {

	private static final Log logger = LogFactory.getLog(OpenTelemetryTracingAutoConfiguration.class);

	private final TracingProperties tracingProperties;

	OpenTelemetryTracingAutoConfiguration(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
		if (!CollectionUtils.isEmpty(this.tracingProperties.getBaggage().getLocalFields())) {
			logger.warn("Local fields are not supported when using OpenTelemetry!");
		}
	}

	@Bean
	@ConditionalOnMissingBean
	SdkTracerProvider otelSdkTracerProvider(Resource resource, SpanProcessors spanProcessors, Sampler sampler,
			ObjectProvider<SdkTracerProviderBuilderCustomizer> customizers) {
		SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setSampler(sampler).setResource(resource);
		spanProcessors.forEach(builder::addSpanProcessor);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
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
		Sampler rootSampler = Sampler.traceIdRatioBased(this.tracingProperties.getSampling().getProbability());
		return Sampler.parentBased(rootSampler);
	}

	@Bean
	@ConditionalOnMissingBean
	SpanProcessors spanProcessors(ObjectProvider<SpanProcessor> spanProcessors) {
		return SpanProcessors.of(spanProcessors.orderedStream().toList());
	}

	@Bean
	BatchSpanProcessor otelSpanProcessor(SpanExporters spanExporters,
			ObjectProvider<SpanExportingPredicate> spanExportingPredicates, ObjectProvider<SpanReporter> spanReporters,
			ObjectProvider<SpanFilter> spanFilters, ObjectProvider<MeterProvider> meterProvider) {
		BatchSpanProcessorBuilder builder = BatchSpanProcessor
			.builder(new CompositeSpanExporter(spanExporters.list(), spanExportingPredicates.orderedStream().toList(),
					spanReporters.orderedStream().toList(), spanFilters.orderedStream().toList()));
		meterProvider.ifAvailable(builder::setMeterProvider);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	SpanExporters spanExporters(ObjectProvider<SpanExporter> spanExporters) {
		return SpanExporters.of(spanExporters.orderedStream().toList());
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
		List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
		List<String> tagFields = this.tracingProperties.getBaggage().getTagFields();
		return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
				new OtelBaggageManager(otelCurrentTraceContext, remoteFields, tagFields));
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
	OtelCurrentTraceContext otelCurrentTraceContext() {
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
