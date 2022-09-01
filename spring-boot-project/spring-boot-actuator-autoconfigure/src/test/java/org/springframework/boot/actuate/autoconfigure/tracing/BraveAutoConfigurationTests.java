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

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.baggage.BaggageField;
import brave.baggage.CorrelationScopeConfig;
import brave.http.HttpClientHandler;
import brave.http.HttpClientRequest;
import brave.http.HttpClientResponse;
import brave.http.HttpServerHandler;
import brave.http.HttpServerRequest;
import brave.http.HttpServerResponse;
import brave.http.HttpTracing;
import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation.Factory;
import brave.sampler.Sampler;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveHttpClientHandler;
import io.micrometer.tracing.brave.bridge.BraveHttpServerHandler;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static brave.propagation.CurrentTraceContext.Scope.NOOP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BraveAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class BraveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class));

	@Test
	void shouldSupplyBraveBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Tracing.class);
			assertThat(context).hasSingleBean(Tracer.class);
			assertThat(context).hasSingleBean(CurrentTraceContext.class);
			assertThat(context).hasSingleBean(Factory.class);
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasSingleBean(HttpTracing.class);
			assertThat(context).hasSingleBean(HttpServerHandler.class);
			assertThat(context).hasSingleBean(HttpClientHandler.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBraveBeans() {
		this.contextRunner.withUserConfiguration(CustomBraveConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customTracing");
			assertThat(context).hasSingleBean(Tracing.class);
			assertThat(context).hasBean("customTracer");
			assertThat(context).hasSingleBean(Tracer.class);
			assertThat(context).hasBean("customCurrentTraceContext");
			assertThat(context).hasSingleBean(CurrentTraceContext.class);
			assertThat(context).hasBean("customFactory");
			assertThat(context).hasSingleBean(Factory.class);
			assertThat(context).hasBean("customSampler");
			assertThat(context).hasSingleBean(Sampler.class);
			assertThat(context).hasBean("customHttpTracing");
			assertThat(context).hasSingleBean(HttpTracing.class);
			assertThat(context).hasBean("customHttpServerHandler");
			assertThat(context).hasSingleBean(HttpServerHandler.class);
			assertThat(context).hasBean("customHttpClientHandler");
			assertThat(context).hasSingleBean(HttpClientHandler.class);
		});
	}

	@Test
	void shouldSupplyMicrometerBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BraveTracer.class);
			assertThat(context).hasSingleBean(BraveBaggageManager.class);
			assertThat(context).hasSingleBean(BraveHttpServerHandler.class);
			assertThat(context).hasSingleBean(BraveHttpClientHandler.class);
		});
	}

	@Test
	void shouldBackOffOnCustomMicrometerBraveBeans() {
		this.contextRunner.withUserConfiguration(CustomMicrometerBraveConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customBraveTracer");
			assertThat(context).hasSingleBean(BraveTracer.class);
			assertThat(context).hasBean("customBraveBaggageManager");
			assertThat(context).hasSingleBean(BraveBaggageManager.class);
			assertThat(context).hasBean("customBraveHttpServerHandler");
			assertThat(context).hasSingleBean(BraveHttpServerHandler.class);
			assertThat(context).hasBean("customBraveHttpClientHandler");
			assertThat(context).hasSingleBean(BraveHttpClientHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBraveBeansIfBraveIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("brave")).run((context) -> {
			assertThat(context).doesNotHaveBean(Tracing.class);
			assertThat(context).doesNotHaveBean(Tracer.class);
			assertThat(context).doesNotHaveBean(CurrentTraceContext.class);
			assertThat(context).doesNotHaveBean(Factory.class);
			assertThat(context).doesNotHaveBean(Sampler.class);
			assertThat(context).doesNotHaveBean(HttpTracing.class);
			assertThat(context).doesNotHaveBean(HttpServerHandler.class);
			assertThat(context).doesNotHaveBean(HttpClientHandler.class);
		});
	}

	@Test
	void shouldNotSupplyMicrometerBeansIfMicrometerIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer")).run((context) -> {
			assertThat(context).doesNotHaveBean(BraveTracer.class);
			assertThat(context).doesNotHaveBean(BraveBaggageManager.class);
			assertThat(context).doesNotHaveBean(BraveHttpServerHandler.class);
			assertThat(context).doesNotHaveBean(BraveHttpClientHandler.class);
		});
	}

	@Test
	void shouldNotSupplyBraveBeansIfTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.enabled=false").run((context) -> {
			assertThat(context).doesNotHaveBean(Tracing.class);
			assertThat(context).doesNotHaveBean(Tracer.class);
			assertThat(context).doesNotHaveBean(CurrentTraceContext.class);
			assertThat(context).doesNotHaveBean(Factory.class);
			assertThat(context).doesNotHaveBean(Sampler.class);
			assertThat(context).doesNotHaveBean(HttpTracing.class);
			assertThat(context).doesNotHaveBean(HttpServerHandler.class);
			assertThat(context).doesNotHaveBean(HttpClientHandler.class);
		});
	}

	@Nested
	class BaggageTests {

		private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
				.withPropertyValues("management.tracing.baggage.remote-fields=x-vcap-request-id,country-code",
						"management.tracing.baggage.local-fields=bp", "management.tracing.baggage.correlation-fields=country-code,bp");
		static final BaggageField COUNTRY_CODE = BaggageField.create("country-code");
		static final BaggageField BUSINESS_PROCESS = BaggageField.create("bp");

		Span span;

		@BeforeEach
		@AfterEach
		void setup() {
			MDC.clear();
		}

		@Test
		void shouldSetEntriesToMdcFromSpan() {
			contextRunner.run(context -> {
				startSpan(context);
				// can't use NOOP as it is special cased
				try (CurrentTraceContext.Scope scope = scopeDecorator(context).decorateScope(this.span.context(), () -> {
				})) {
					assertThat(MDC.get("traceId")).isEqualTo(this.span.context().traceIdString());
				}

				assertThat(MDC.get("traceId")).isNullOrEmpty();
			});
		}

		private void startSpan(ApplicationContext context) {
			this.span = tracer(context).nextSpan().name("span").start();
		}

		private Tracer tracer(ApplicationContext context) {
			return context.getBean(Tracer.class);
		}

		private CurrentTraceContext.ScopeDecorator scopeDecorator(ApplicationContext context) {
			return context.getBean(CurrentTraceContext.ScopeDecorator.class);
		}

		@Test
		void shouldSetEntriesToMdcFromSpanWithBaggage() {
			contextRunner.run(context -> {
				startSpan(context);
				COUNTRY_CODE.updateValue(this.span.context(), "FO");
				BUSINESS_PROCESS.updateValue(this.span.context(), "ALM");

				try (CurrentTraceContext.Scope scope = scopeDecorator(context).decorateScope(this.span.context(), NOOP)) {
					assertThat(MDC.get(COUNTRY_CODE.name())).isEqualTo("FO");
					assertThat(MDC.get(BUSINESS_PROCESS.name())).isEqualTo("ALM");
				}

				assertThat(MDC.get(COUNTRY_CODE.name())).isNull();
				assertThat(MDC.get(BUSINESS_PROCESS.name())).isNull();
			});
		}

		@Test
		void shouldRemoveEntriesFromMdcForNullSpan() {
			contextRunner.run(context -> {
				startSpan(context);
				COUNTRY_CODE.updateValue(this.span.context(), "FO");

				try (CurrentTraceContext.Scope scope1 = scopeDecorator(context).decorateScope(this.span.context(), NOOP)) {
					assertThat(MDC.get(COUNTRY_CODE.name())).isEqualTo("FO");

					try (CurrentTraceContext.Scope scope2 = scopeDecorator(context).decorateScope(null, NOOP)) {
						assertThat(MDC.get(COUNTRY_CODE.name())).isNullOrEmpty();
					}
				}
			});
		}

		@Test
		void shouldRemoveEntriesFromMdcForNullSpanAndMdcFieldsSetDirectly() {
			contextRunner.run(context -> {
				startSpan(context);
				MDC.put(COUNTRY_CODE.name(), "FO");

				// the span is holding no baggage so it clears the preceding values
				try (CurrentTraceContext.Scope scope = scopeDecorator(context).decorateScope(this.span.context(), NOOP)) {
					assertThat(MDC.get(COUNTRY_CODE.name())).isNullOrEmpty();
				}

				assertThat(MDC.get(COUNTRY_CODE.name())).isEqualTo("FO");

				try (CurrentTraceContext.Scope scope = scopeDecorator(context).decorateScope(null, NOOP)) {
					assertThat(MDC.get(COUNTRY_CODE.name())).isNullOrEmpty();
				}

				assertThat(MDC.get(COUNTRY_CODE.name())).isEqualTo("FO");
			});
		}

		@Test
		void shouldRemoveEntriesFromMdcFromNullSpan() {
			contextRunner.run(context -> {
				startSpan(context);
				MDC.put("traceId", "A");

				// can't use NOOP as it is special cased
				try (CurrentTraceContext.Scope scope = scopeDecorator(context).decorateScope(null, () -> {
				})) {
					assertThat(MDC.get("traceId")).isNullOrEmpty();
				}

				assertThat(MDC.get("traceId")).isEqualTo("A");
			});

		}

		@Test
		void shouldOnlyIncludeWhitelist() {
			contextRunner.run(context -> {
				assertThat(scopeDecorator(context)).extracting("fields")
						.asInstanceOf(InstanceOfAssertFactories.array(CorrelationScopeConfig.SingleCorrelationField[].class))
						.extracting(CorrelationScopeConfig.SingleCorrelationField::name).containsOnly("traceId", "spanId", "bp", COUNTRY_CODE.name());
			});
		}

		@Test
		void shouldPickPreviousMdcEntriesWhenTheirKeysAreWhitelisted() {
			contextRunner.run(context -> {
				startSpan(context);
				MDC.put(COUNTRY_CODE.name(), "FO");

				try (CurrentTraceContext.Scope scope = scopeDecorator(context).decorateScope(this.span.context(), NOOP)) {
					MDC.put(COUNTRY_CODE.name(), "BV");

					assertThat(MDC.get(COUNTRY_CODE.name())).isEqualTo("BV");
				}

				assertThat(MDC.get(COUNTRY_CODE.name())).isEqualTo("FO");

			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomBraveConfiguration {

		@Bean
		Tracing customTracing() {
			return mock(Tracing.class);
		}

		@Bean
		Tracer customTracer() {
			return mock(Tracer.class);
		}

		@Bean
		CurrentTraceContext customCurrentTraceContext() {
			return mock(CurrentTraceContext.class);
		}

		@Bean
		Factory customFactory() {
			return mock(Factory.class);
		}

		@Bean
		Sampler customSampler() {
			return mock(Sampler.class);
		}

		@Bean
		HttpTracing customHttpTracing() {
			return mock(HttpTracing.class);
		}

		@Bean
		HttpServerHandler<HttpServerRequest, HttpServerResponse> customHttpServerHandler() {
			HttpTracing httpTracing = mock(HttpTracing.class, Answers.RETURNS_MOCKS);
			return HttpServerHandler.create(httpTracing);
		}

		@Bean
		HttpClientHandler<HttpClientRequest, HttpClientResponse> customHttpClientHandler() {
			HttpTracing httpTracing = mock(HttpTracing.class, Answers.RETURNS_MOCKS);
			return HttpClientHandler.create(httpTracing);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomMicrometerBraveConfiguration {

		@Bean
		BraveTracer customBraveTracer() {
			return mock(BraveTracer.class);
		}

		@Bean
		BraveBaggageManager customBraveBaggageManager() {
			return mock(BraveBaggageManager.class);
		}

		@Bean
		BraveHttpServerHandler customBraveHttpServerHandler() {
			return mock(BraveHttpServerHandler.class);
		}

		@Bean
		BraveHttpClientHandler customBraveHttpClientHandler() {
			return mock(BraveHttpClientHandler.class);
		}

	}

}
