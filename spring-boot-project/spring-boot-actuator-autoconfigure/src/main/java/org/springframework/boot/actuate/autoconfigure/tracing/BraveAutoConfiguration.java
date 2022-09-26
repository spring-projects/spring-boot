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

import brave.Tracing;
import brave.Tracing.Builder;
import brave.TracingCustomizer;
import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagationConfig;
import brave.baggage.BaggagePropagationCustomizer;
import brave.baggage.CorrelationScopeConfig;
import brave.baggage.CorrelationScopeCustomizer;
import brave.baggage.CorrelationScopeDecorator;
import brave.context.slf4j.MDCScopeDecorator;
import brave.handler.SpanHandler;
import brave.http.HttpClientHandler;
import brave.http.HttpClientRequest;
import brave.http.HttpClientResponse;
import brave.http.HttpServerHandler;
import brave.http.HttpServerRequest;
import brave.http.HttpServerResponse;
import brave.http.HttpTracing;
import brave.propagation.B3Propagation;
import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.ScopeDecorator;
import brave.propagation.CurrentTraceContextCustomizer;
import brave.propagation.Propagation;
import brave.propagation.Propagation.Factory;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveHttpClientHandler;
import io.micrometer.tracing.brave.bridge.BraveHttpServerHandler;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.W3CPropagation;
import org.slf4j.MDC;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Brave.
 *
 * @author Moritz Halbritter
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
@AutoConfiguration(before = MicrometerTracingAutoConfiguration.class)
@ConditionalOnClass(brave.Tracer.class)
@EnableConfigurationProperties(TracingProperties.class)
@ConditionalOnEnabledTracing
public class BraveAutoConfiguration {

	/**
	 * Default value for application name if {@code spring.application.name} is not set.
	 */
	private static final String DEFAULT_APPLICATION_NAME = "application";

	@Bean
	@ConditionalOnMissingBean
	public Tracing braveTracing(Environment environment, List<SpanHandler> spanHandlers,
			List<TracingCustomizer> tracingCustomizers, CurrentTraceContext currentTraceContext,
			Factory propagationFactory, Sampler sampler) {
		String applicationName = environment.getProperty("spring.application.name", DEFAULT_APPLICATION_NAME);
		Builder builder = Tracing.newBuilder().currentTraceContext(currentTraceContext)
				.propagationFactory(propagationFactory).sampler(sampler).localServiceName(applicationName);
		for (SpanHandler spanHandler : spanHandlers) {
			builder.addSpanHandler(spanHandler);
		}
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
		for (ScopeDecorator scopeDecorator : scopeDecorators) {
			builder.addScopeDecorator(scopeDecorator);
		}
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
	@ConditionalOnMissingBean
	public HttpTracing httpTracing(Tracing tracing) {
		return HttpTracing.newBuilder(tracing).build();
	}

	@Bean
	@ConditionalOnMissingBean
	public HttpServerHandler<HttpServerRequest, HttpServerResponse> httpServerHandler(HttpTracing httpTracing) {
		return HttpServerHandler.create(httpTracing);
	}

	@Bean
	@ConditionalOnMissingBean
	public HttpClientHandler<HttpClientRequest, HttpClientResponse> httpClientHandler(HttpTracing httpTracing) {
		return HttpClientHandler.create(httpTracing);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("io.micrometer.tracing.brave.bridge.BraveTracer")
	static class BraveMicrometerMissing {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "B3", matchIfMissing = true)
		Factory bravePropagationFactory() {
			return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(BraveTracer.class)
	static class BraveMicrometer {

		private static final BraveBaggageManager BRAVE_BAGGAGE_MANAGER = new BraveBaggageManager();

		@Bean
		@ConditionalOnMissingBean
		BraveTracer braveTracerBridge(brave.Tracer tracer, CurrentTraceContext currentTraceContext) {
			return new BraveTracer(tracer, new BraveCurrentTraceContext(currentTraceContext), BRAVE_BAGGAGE_MANAGER);
		}

		@Bean
		@ConditionalOnMissingBean
		BravePropagator bravePropagator(Tracing tracing) {
			return new BravePropagator(tracing);
		}

		@Bean
		@ConditionalOnMissingBean
		BraveHttpServerHandler braveHttpServerHandler(
				HttpServerHandler<HttpServerRequest, HttpServerResponse> httpServerHandler) {
			return new BraveHttpServerHandler(httpServerHandler);
		}

		@Bean
		@ConditionalOnMissingBean
		BraveHttpClientHandler braveHttpClientHandler(
				HttpClientHandler<HttpClientRequest, HttpClientResponse> httpClientHandler) {
			return new BraveHttpClientHandler(httpClientHandler);
		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(value = "management.tracing.baggage.enabled", havingValue = "false",
				matchIfMissing = true)
		static class BraveNoBaggageConfiguration {

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "W3C",
					matchIfMissing = true)
			Factory w3cPropagationNoBaggageFactory() {
				return new W3CPropagation(BRAVE_BAGGAGE_MANAGER, List.of()); // TODO: Use
																				// snapshots
																				// of
																				// tracing
																				// to not
																				// use
																				// baggage
																				// for W3C
			}

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "B3")
			Factory b3PropagationNoBaggageFactory() {
				return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build();
			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnProperty(value = "management.tracing.baggage.enabled", matchIfMissing = true)
		static class BraveBaggageConfiguration {

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "W3C",
					matchIfMissing = true)
			BaggagePropagation.FactoryBuilder w3cPropagationFactory() {
				return BaggagePropagation.newFactoryBuilder(new W3CPropagation(BRAVE_BAGGAGE_MANAGER, List.of()));
			}

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation.type", havingValue = "B3")
			BaggagePropagation.FactoryBuilder b3PropagationFactory() {
				return BaggagePropagation.newFactoryBuilder(
						B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build());
			}

			@Bean
			@ConditionalOnMissingBean
			Propagation.Factory micrometerTracingPropagationWithBaggage(
					BaggagePropagation.FactoryBuilder factoryBuilder, TracingProperties tracingProperties,
					ObjectProvider<List<BaggagePropagationCustomizer>> baggagePropagationCustomizers) {
				List<String> remoteFields = tracingProperties.getBaggage().getRemoteFields();
				for (String fieldName : remoteFields) {
					factoryBuilder
							.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create(fieldName)));
				}
				baggagePropagationCustomizers.ifAvailable(
						(customizers) -> customizers.forEach((customizer) -> customizer.customize(factoryBuilder)));
				return factoryBuilder.build();
			}

			@Bean
			@ConditionalOnMissingBean(CorrelationScopeDecorator.class)
			@ConditionalOnBean(CorrelationScopeDecorator.Builder.class)
			@ConditionalOnProperty(value = "management.tracing.baggage.correlation.enabled", matchIfMissing = true)
			ScopeDecorator correlationFieldsCorrelationScopeDecorator(TracingProperties properties,
					ObjectProvider<List<CorrelationScopeCustomizer>> correlationScopeCustomizers,
					CorrelationScopeDecorator.Builder builder) {
				List<String> correlationFields = properties.getBaggage().getCorrelation().getFields();
				for (String field : correlationFields) {
					builder.add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageField.create(field))
							.flushOnUpdate().build());
				}
				correlationScopeCustomizers.ifAvailable(
						(customizers) -> customizers.forEach((customizer) -> customizer.customize(builder)));
				return builder.build();
			}

			@Bean
			@ConditionalOnMissingBean(CorrelationScopeDecorator.class)
			@ConditionalOnBean(CorrelationScopeDecorator.Builder.class)
			@ConditionalOnProperty(value = "management.tracing.baggage.correlation.enabled", havingValue = "false")
			ScopeDecorator noCorrelationFieldsCorrelationScopeDecorator(CorrelationScopeDecorator.Builder builder,
					ObjectProvider<List<CorrelationScopeCustomizer>> correlationScopeCustomizers) {
				correlationScopeCustomizers.ifAvailable(
						(customizers) -> customizers.forEach((customizer) -> customizer.customize(builder)));
				return builder.build();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CorrelationScopeDecoratorConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(MDC.class)
		CorrelationScopeDecorator.Builder mdcCorrelationScopeDecoratorBuilder() {
			return MDCScopeDecorator.newBuilder();
		}

		@Bean
		@ConditionalOnMissingBean(CorrelationScopeDecorator.class)
		@ConditionalOnBean(CorrelationScopeDecorator.Builder.class)
		@ConditionalOnMissingClass("io.micrometer.tracing.brave.bridge.BraveTracer")
		ScopeDecorator correlationScopeDecorator(CorrelationScopeDecorator.Builder builder,
				ObjectProvider<List<CorrelationScopeCustomizer>> correlationScopeCustomizers) {
			correlationScopeCustomizers
					.ifAvailable((customizers) -> customizers.forEach((customizer) -> customizer.customize(builder)));
			return builder.build();
		}

	}

}
