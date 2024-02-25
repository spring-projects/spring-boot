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
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
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
import io.opentelemetry.context.ContextStorage;
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
 * @since 3.0.0
 */
@AutoConfiguration(value = "openTelemetryTracingAutoConfiguration",
		before = { MicrometerTracingAutoConfiguration.class, NoopTracerAutoConfiguration.class })
@ConditionalOnClass({ OtelTracer.class, SdkTracerProvider.class, OpenTelemetry.class })
@EnableConfigurationProperties(TracingProperties.class)
@Import({ OpenTelemetryPropagationConfigurations.PropagationWithoutBaggage.class,
		OpenTelemetryPropagationConfigurations.PropagationWithBaggage.class,
		OpenTelemetryPropagationConfigurations.NoPropagation.class })
public class OpenTelemetryAutoConfiguration {

	private static final Log logger = LogFactory.getLog(OpenTelemetryAutoConfiguration.class);

	private final TracingProperties tracingProperties;

	/**
	 * Initializes the OpenTelemetry auto-configuration with the provided tracing
	 * properties.
	 * @param tracingProperties the tracing properties to configure OpenTelemetry
	 */
	OpenTelemetryAutoConfiguration(TracingProperties tracingProperties) {
		this.tracingProperties = tracingProperties;
		if (!CollectionUtils.isEmpty(this.tracingProperties.getBaggage().getLocalFields())) {
			logger.warn("Local fields are not supported when using OpenTelemetry!");
		}
	}

	/**
	 * Creates an instance of {@link SdkTracerProvider} if no other bean of the same type
	 * is present.
	 * @param resource The {@link Resource} to be used by the {@link SdkTracerProvider}.
	 * @param spanProcessors The {@link SpanProcessors} to be added to the
	 * {@link SdkTracerProvider}.
	 * @param sampler The {@link Sampler} to be set for the {@link SdkTracerProvider}.
	 * @param customizers The {@link SdkTracerProviderBuilderCustomizer}s to customize the
	 * {@link SdkTracerProviderBuilder}.
	 * @return An instance of {@link SdkTracerProvider} configured with the provided
	 * parameters.
	 */
	@Bean
	@ConditionalOnMissingBean
	SdkTracerProvider otelSdkTracerProvider(Resource resource, SpanProcessors spanProcessors, Sampler sampler,
			ObjectProvider<SdkTracerProviderBuilderCustomizer> customizers) {
		SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setSampler(sampler).setResource(resource);
		spanProcessors.forEach(builder::addSpanProcessor);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	/**
	 * Creates an instance of ContextPropagators with the provided TextMapPropagator
	 * objects. If no TextMapPropagator objects are provided, a default instance will be
	 * created.
	 * @param textMapPropagators The TextMapPropagator objects to be used for context
	 * propagation.
	 * @return The created instance of ContextPropagators.
	 */
	@Bean
	@ConditionalOnMissingBean
	ContextPropagators otelContextPropagators(ObjectProvider<TextMapPropagator> textMapPropagators) {
		return ContextPropagators.create(TextMapPropagator.composite(textMapPropagators.orderedStream().toList()));
	}

	/**
	 * Returns a Sampler bean for OpenTelemetry tracing.
	 *
	 * This method is annotated with @Bean to indicate that it is a bean definition
	 * method.
	 *
	 * This method is annotated with @ConditionalOnMissingBean to ensure that the bean is
	 * only created if there is no existing bean of the same type.
	 *
	 * The method creates a Sampler bean based on the configured sampling probability from
	 * the tracing properties.
	 *
	 * The Sampler bean is created using the traceIdRatioBased method of the Sampler
	 * class, passing in the sampling probability from the tracing properties.
	 *
	 * The created Sampler bean is then wrapped in a parentBased Sampler using the
	 * parentBased method of the Sampler class.
	 * @return the created Sampler bean for OpenTelemetry tracing
	 */
	@Bean
	@ConditionalOnMissingBean
	Sampler otelSampler() {
		Sampler rootSampler = Sampler.traceIdRatioBased(this.tracingProperties.getSampling().getProbability());
		return Sampler.parentBased(rootSampler);
	}

	/**
	 * Creates a {@link SpanProcessors} bean if no other bean of type
	 * {@link SpanProcessor} is present.
	 * @param spanProcessors the object provider for {@link SpanProcessor} beans
	 * @return the {@link SpanProcessors} bean
	 */
	@Bean
	@ConditionalOnMissingBean
	SpanProcessors spanProcessors(ObjectProvider<SpanProcessor> spanProcessors) {
		return SpanProcessors.of(spanProcessors.orderedStream().toList());
	}

	/**
	 * Creates a BatchSpanProcessor bean for exporting spans.
	 * @param spanExporters the span exporters to be used for exporting spans
	 * @param spanExportingPredicates the span exporting predicates to be used for
	 * filtering spans to be exported
	 * @param spanReporters the span reporters to be used for reporting spans
	 * @param spanFilters the span filters to be used for filtering spans
	 * @param meterProvider the meter provider to be used for providing meters
	 * @return the created BatchSpanProcessor bean
	 */
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

	/**
	 * Creates a {@link SpanExporters} bean if no other bean of type {@link SpanExporter}
	 * is present.
	 * @param spanExporters the object provider for {@link SpanExporter} beans
	 * @return the {@link SpanExporters} bean
	 */
	@Bean
	@ConditionalOnMissingBean
	SpanExporters spanExporters(ObjectProvider<SpanExporter> spanExporters) {
		return SpanExporters.of(spanExporters.orderedStream().toList());
	}

	/**
	 * Creates a Tracer bean if no other bean of type Tracer is present.
	 * @param openTelemetry the OpenTelemetry instance to use
	 * @return the Tracer bean
	 */
	@Bean
	@ConditionalOnMissingBean
	Tracer otelTracer(OpenTelemetry openTelemetry) {
		return openTelemetry.getTracer("org.springframework.boot", SpringBootVersion.getVersion());
	}

	/**
	 * Creates an instance of OtelTracer if no bean of type Tracer is present.
	 * @param tracer The Tracer instance to be used.
	 * @param eventPublisher The EventPublisher instance to be used.
	 * @param otelCurrentTraceContext The OtelCurrentTraceContext instance to be used.
	 * @return An instance of OtelTracer.
	 */
	@Bean
	@ConditionalOnMissingBean(io.micrometer.tracing.Tracer.class)
	OtelTracer micrometerOtelTracer(Tracer tracer, EventPublisher eventPublisher,
			OtelCurrentTraceContext otelCurrentTraceContext) {
		List<String> remoteFields = this.tracingProperties.getBaggage().getRemoteFields();
		List<String> tagFields = this.tracingProperties.getBaggage().getTagFields();
		return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
				new OtelBaggageManager(otelCurrentTraceContext, remoteFields, tagFields));
	}

	/**
	 * Creates an instance of OtelPropagator if no other bean of the same type is present.
	 * @param contextPropagators The ContextPropagators instance to be used by the
	 * OtelPropagator.
	 * @param tracer The Tracer instance to be used by the OtelPropagator.
	 * @return An instance of OtelPropagator.
	 */
	@Bean
	@ConditionalOnMissingBean
	OtelPropagator otelPropagator(ContextPropagators contextPropagators, Tracer tracer) {
		return new OtelPropagator(contextPropagators, tracer);
	}

	/**
	 * Creates an instance of {@link EventPublisher} if no other bean of type
	 * {@link EventPublisher} is present.
	 * @param eventListeners the list of {@link EventListener} beans to be used by the
	 * {@link OTelEventPublisher}
	 * @return an instance of {@link EventPublisher} implemented by
	 * {@link OTelEventPublisher}
	 */
	@Bean
	@ConditionalOnMissingBean
	EventPublisher otelTracerEventPublisher(List<EventListener> eventListeners) {
		return new OTelEventPublisher(eventListeners);
	}

	/**
	 * Creates an instance of OtelCurrentTraceContext if no other bean of the same type is
	 * present. Adds an EventPublishingContextWrapper to the ContextStorage.
	 * @param publisher the EventPublisher used by the EventPublishingContextWrapper
	 * @return an instance of OtelCurrentTraceContext
	 */
	@Bean
	@ConditionalOnMissingBean
	OtelCurrentTraceContext otelCurrentTraceContext(EventPublisher publisher) {
		ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
		return new OtelCurrentTraceContext();
	}

	/**
	 * Creates a new instance of Slf4JEventListener if no other bean of the same type is
	 * present in the application context. This bean is conditionally created only if
	 * there is no other bean of the same type already defined.
	 * @return a new instance of Slf4JEventListener
	 */
	@Bean
	@ConditionalOnMissingBean
	Slf4JEventListener otelSlf4JEventListener() {
		return new Slf4JEventListener();
	}

	/**
	 * Creates a new instance of {@link OtelSpanCustomizer} if no other bean of type
	 * {@link SpanCustomizer} is present.
	 * @return the created instance of {@link OtelSpanCustomizer}
	 */
	@Bean
	@ConditionalOnMissingBean(SpanCustomizer.class)
	OtelSpanCustomizer otelSpanCustomizer() {
		return new OtelSpanCustomizer();
	}

	/**
	 * OTelEventPublisher class.
	 */
	static class OTelEventPublisher implements EventPublisher {

		private final List<EventListener> listeners;

		/**
		 * Constructs a new OTelEventPublisher with the specified list of EventListeners.
		 * @param listeners the list of EventListeners to be registered with the publisher
		 */
		OTelEventPublisher(List<EventListener> listeners) {
			this.listeners = listeners;
		}

		/**
		 * Publishes an event to all registered listeners.
		 * @param event the event to be published
		 */
		@Override
		public void publishEvent(Object event) {
			for (EventListener listener : this.listeners) {
				listener.onEvent(event);
			}
		}

	}

}
