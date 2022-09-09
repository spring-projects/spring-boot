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

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import brave.propagation.aws.AWSPropagation;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.SamplerFunction;
import io.micrometer.tracing.otel.bridge.BaggageTaggingSpanProcessor;
import io.micrometer.tracing.otel.bridge.DefaultHttpClientAttributesGetter;
import io.micrometer.tracing.otel.bridge.DefaultHttpServerAttributesExtractor;
import io.micrometer.tracing.otel.bridge.EventListener;
import io.micrometer.tracing.otel.bridge.EventPublishingContextWrapper;
import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelHttpClientHandler;
import io.micrometer.tracing.otel.bridge.OtelHttpServerHandler;
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
import io.opentelemetry.extension.aws.AwsXrayPropagator;
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
import org.slf4j.MDC;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configurations for OpenTelemetry. Those are imported by
 * {@link OpenTelemetryAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Marcin Grzejszczak
 */
class OpenTelemetryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SdkTracerProvider.class)
	@EnableConfigurationProperties(TracingProperties.class)
	static class SdkConfiguration {

		/**
		 * Default value for application name if {@code spring.application.name} is not
		 * set.
		 */
		private static final String DEFAULT_APPLICATION_NAME = "application";

		@Bean
		@ConditionalOnMissingBean
		OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider, ContextPropagators contextPropagators) {
			return OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).setPropagators(contextPropagators)
					.build();
		}

		@Bean
		@ConditionalOnMissingBean
		SdkTracerProvider otelSdkTracerProvider(Environment environment, List<SpanProcessor> spanProcessors,
				Sampler sampler) {
			String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
			SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setSampler(sampler)
					.setResource(Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, applicationName)));
			for (SpanProcessor spanProcessor : spanProcessors) {
				builder.addSpanProcessor(spanProcessor);
			}
			return builder.build();
		}

		@Bean
		@ConditionalOnMissingBean
		ContextPropagators otelContextPropagators(List<TextMapPropagator> textMapPropagators) {
			return ContextPropagators.create(TextMapPropagator.composite(textMapPropagators));
		}

		@Bean
		@ConditionalOnMissingBean
		Sampler otelSampler(TracingProperties properties) {
			return Sampler.traceIdRatioBased(properties.getSampling().getProbability());
		}

		@Bean
		SpanProcessor otelSpanProcessor(List<SpanExporter> spanExporter) {
			return SpanProcessor.composite(spanExporter.stream()
					.map((exporter) -> BatchSpanProcessor.builder(exporter).build()).collect(Collectors.toList()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Tracer.class)
	static class TracerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(OpenTelemetry.class)
		Tracer otelTracer(OpenTelemetry openTelemetry) {
			return openTelemetry.getTracer("org.springframework.boot", SpringBootVersion.getVersion());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(OtelTracer.class)
	@EnableConfigurationProperties(TracingProperties.class)
	static class MicrometerConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(Tracer.class)
		OtelTracer micrometerOtelTracer(Tracer tracer, EventPublisher eventPublisher,
				OtelCurrentTraceContext otelCurrentTraceContext) {
			return new OtelTracer(tracer, otelCurrentTraceContext, eventPublisher,
					new OtelBaggageManager(otelCurrentTraceContext, List.of(), List.of()));
		}

		@Bean
		@ConditionalOnMissingBean
		EventPublisher otelTracerEventPublisher(TracingProperties tracingProperties) {
			return new OTelEventPublisher(List.of(new Slf4JEventListener(),
					new Slf4JBaggageEventListener(tracingProperties.getBaggage().getCorrelationFields())));
		}

		@Bean
		@ConditionalOnMissingBean
		OtelCurrentTraceContext otelCurrentTraceContext(EventPublisher publisher) {
			ContextStorage.addWrapper(new EventPublishingContextWrapper(publisher));
			return new OtelCurrentTraceContext();
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(OpenTelemetry.class)
		OtelHttpClientHandler otelHttpClientHandler(OpenTelemetry openTelemetry) {
			return new OtelHttpClientHandler(openTelemetry, null, null, SamplerFunction.deferDecision(),
					new DefaultHttpClientAttributesGetter());
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(OpenTelemetry.class)
		OtelHttpServerHandler otelHttpServerHandler(OpenTelemetry openTelemetry) {
			return new OtelHttpServerHandler(openTelemetry, null, null, Pattern.compile(""),
					new DefaultHttpServerAttributesExtractor());
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

		@Configuration(proxyBeanMethods = false)
		static class PropagationConfiguration {

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "B3", matchIfMissing = true)
			B3Propagator b3TextMapPropagator() {
				return B3Propagator.injectingSingleHeader();
			}

			@Bean
			@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "AWS")
			@ConditionalOnClass(AWSPropagation.class)
			AwsXrayPropagator awsTextMapPropagator() {
				return AwsXrayPropagator.getInstance();
			}

			@Configuration(proxyBeanMethods = false)
			@ConditionalOnProperty(value = "management.tracing.baggage.enabled", havingValue = "false")
			static class NoBaggagePropagatorConfiguration {

				@Bean
				@ConditionalOnMissingBean
				@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "W3C")
				W3CTraceContextPropagator w3cTextMapPropagator() {
					return W3CTraceContextPropagator.getInstance();
				}

			}

			@Configuration(proxyBeanMethods = false)
			@ConditionalOnProperty(value = "management.tracing.baggage.enabled", matchIfMissing = true)
			static class BaggagePropagatorConfiguration {

				@Bean
				@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "B3")
				BaggageTextMapPropagator baggageTextMapPropagator(TracingProperties properties,
						BaggageManager baggageManager) {
					return new BaggageTextMapPropagator(properties.getBaggage().getRemoteFields(), baggageManager);
				}

				@Bean
				@ConditionalOnMissingBean
				@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "W3C")
				TextMapPropagator w3cTextMapPropagator() {
					return TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(),
							W3CBaggagePropagator.getInstance());
				}

				@Bean
				@Conditional(BaggageTagSpanHandlerCondition.class)
				BaggageTaggingSpanProcessor baggageTaggingSpanProcessor(TracingProperties properties) {
					return new BaggageTaggingSpanProcessor(properties.getBaggage().getTagFields());
				}

				/**
				 * We need a special condition as it users could use either comma or yaml
				 * encoding.
				 */
				static class BaggageTagSpanHandlerCondition extends AnyNestedCondition {

					BaggageTagSpanHandlerCondition() {
						super(ConfigurationPhase.PARSE_CONFIGURATION);
					}

					@ConditionalOnProperty("management.tracing.baggage.tag-fields")
					static class TagFieldsProperty {

					}

					@ConditionalOnProperty("management.tracing.baggage.tag-fields[0]")
					static class TagFieldsYamlListProperty {

					}

				}

				@Configuration(proxyBeanMethods = false)
				@ConditionalOnClass(MDC.class)
				static class Slf4jConfiguration {

					@Bean
					@ConditionalOnMissingBean
					OpenTelemetrySlf4jBaggageApplicationListener otelSlf4jBaggageApplicationListener(
							TracingProperties tracingProperties) {
						return new OpenTelemetrySlf4jBaggageApplicationListener(
								tracingProperties.getBaggage().getCorrelationFields());
					}

				}

			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(MDC.class)
		static class Slf4jConfiguration {

			@Bean
			@ConditionalOnMissingBean
			OpenTelemetrySlf4jApplicationListener otelSlf4jApplicationListener() {
				return new OpenTelemetrySlf4jApplicationListener();
			}

		}

	}

}
