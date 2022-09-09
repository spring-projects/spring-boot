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

import brave.Tags;
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
import brave.handler.MutableSpan;
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
import brave.propagation.TraceContext;
import brave.propagation.aws.AWSPropagation;
import brave.sampler.Sampler;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveHttpClientHandler;
import io.micrometer.tracing.brave.bridge.BraveHttpServerHandler;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.W3CPropagation;
import org.slf4j.MDC;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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
	@Conditional(OnNonCustomPropagationCondition.class)
	static class BraveMicrometerMissing {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "B3", matchIfMissing = true)
		Factory bravePropagationFactory() {
			return B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build();
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "AWS")
		@ConditionalOnClass(AWSPropagation.class)
		Factory awsPropagationFactory() {
			return AWSPropagation.FACTORY;
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
		@ConditionalOnProperty(value = "management.tracing.baggage.enabled", matchIfMissing = true)
		@Conditional(OnNonCustomPropagationCondition.class)
		static class BraveBaggageConfiguration {

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "W3C", matchIfMissing = true)
			BaggagePropagation.FactoryBuilder w3cPropagationFactory(TracingProperties tracingProperties) {
				return BaggagePropagation.newFactoryBuilder(
						new W3CPropagation(BRAVE_BAGGAGE_MANAGER, tracingProperties.getBaggage().getLocalFields()));
			}

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "B3")
			BaggagePropagation.FactoryBuilder b3PropagationFactory() {
				return BaggagePropagation.newFactoryBuilder(
						B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.SINGLE_NO_PARENT).build());
			}

			@Bean
			@ConditionalOnMissingBean
			@ConditionalOnProperty(value = "management.tracing.propagation", havingValue = "AWS")
			@ConditionalOnClass(AWSPropagation.class)
			BaggagePropagation.FactoryBuilder awsPropagationFactory() {
				return BaggagePropagation.newFactoryBuilder(AWSPropagation.FACTORY);
			}

			@Bean
			@ConditionalOnMissingBean
			Propagation.Factory micrometerTracingPropagationWithBaggage(
					BaggagePropagation.FactoryBuilder factoryBuilder, TracingProperties tracingProperties,
					ObjectProvider<List<BaggagePropagationCustomizer>> baggagePropagationCustomizers) {
				List<String> localFields = tracingProperties.getBaggage().getLocalFields();
				for (String fieldName : localFields) {
					factoryBuilder
							.add(BaggagePropagationConfig.SingleBaggageField.local(BaggageField.create(fieldName)));
				}
				List<String> remoteFields = tracingProperties.getBaggage().getRemoteFields();
				for (String fieldName : remoteFields) {
					factoryBuilder
							.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create(fieldName)));
				}
				baggagePropagationCustomizers.ifAvailable(
						(customizers) -> customizers.forEach((customizer) -> customizer.customize(factoryBuilder)));
				return factoryBuilder.build();
			}

			/**
			 * This has to be conditional as it creates a bean of type
			 * {@link SpanHandler}.
			 *
			 * <p>
			 * {@link SpanHandler} beans, even if {@link SpanHandler#NOOP}, can trigger
			 * {@code SamplerCondition}
			 */
			@Configuration(proxyBeanMethods = false)
			@Conditional(BaggageTagSpanHandlerCondition.class)
			static class BaggageTagSpanHandlerConfiguration {

				@Bean
				SpanHandler baggageTagSpanHandler(TracingProperties properties) {
					List<String> tagFields = properties.getBaggage().getTagFields();
					if (tagFields.isEmpty()) {
						return SpanHandler.NOOP; // Brave ignores these
					}
					return new BaggageTagSpanHandler(
							tagFields.stream().map(BaggageField::create).toArray(BaggageField[]::new));
				}

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

			static final class BaggageTagSpanHandler extends SpanHandler {

				final BaggageField[] fieldsToTag;

				BaggageTagSpanHandler(BaggageField[] fieldsToTag) {
					this.fieldsToTag = fieldsToTag;
				}

				@Override
				public boolean end(TraceContext context, MutableSpan span, Cause cause) {
					for (BaggageField field : this.fieldsToTag) {
						Tags.BAGGAGE_FIELD.tag(field, context, span);
					}
					return true;
				}

			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PropagationConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnClass(MDC.class)
		CorrelationScopeDecorator.Builder correlationScopeDecoratorBuilder() {
			return MDCScopeDecorator.newBuilder();
		}

		@Bean
		@ConditionalOnMissingBean(CorrelationScopeDecorator.class)
		@ConditionalOnBean(CorrelationScopeDecorator.Builder.class)
		@ConditionalOnProperty(value = "management.tracing.baggage.correlation-enabled", matchIfMissing = true)
		ScopeDecorator correlationScopeDecorator(TracingProperties properties,
				ObjectProvider<List<CorrelationScopeCustomizer>> correlationScopeCustomizers) {
			List<String> correlationFields = properties.getBaggage().getCorrelationFields();
			CorrelationScopeDecorator.Builder builder = MDCScopeDecorator.newBuilder();
			for (String field : correlationFields) {
				builder.add(CorrelationScopeConfig.SingleCorrelationField.newBuilder(BaggageField.create(field))
						.flushOnUpdate().build());
			}
			correlationScopeCustomizers
					.ifAvailable((customizers) -> customizers.forEach((customizer) -> customizer.customize(builder)));
			return builder.build();
		}

	}

}
