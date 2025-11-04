/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.tracing.brave.autoconfigure;

import java.util.function.Supplier;

import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.BaggageManager;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.MDC;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Baggage propagation with Brave using W3C and B3 propagation formats.
 *
 * @author Marcin Grzejszczak
 * @author Moritz Halbritter
 */
@ForkedClassPath
class BraveBaggagePropagationIntegrationTests {

	private static final String COUNTRY_CODE = "country-code";

	private static final String BUSINESS_PROCESS = "bp";

	@BeforeEach
	@AfterEach
	void setup() {
		MDC.clear();
	}

	@ParameterizedTest
	@EnumSource
	void shouldSetEntriesToMdcFromSpanWithBaggage(AutoConfig autoConfig) {
		autoConfig.get().run((context) -> {
			Tracer tracer = tracer(context);
			Span span = createSpan(tracer);
			BaggageManager baggageManager = baggageManager(context);
			try (Tracer.SpanInScope scope = tracer.withSpan(span.start())) {
				assertMdcValue("traceId", span.context().traceId());
				try (BaggageInScope fo = baggageManager.createBaggageInScope(span.context(), COUNTRY_CODE, "FO");
						BaggageInScope alm = baggageManager.createBaggageInScope(span.context(), BUSINESS_PROCESS,
								"ALM")) {
					assertMdcValue(COUNTRY_CODE, "FO");
					assertMdcValue(BUSINESS_PROCESS, "ALM");
				}
			}
			finally {
				span.end();
			}
			assertThatMdcContainsUnsetTraceId();
			assertUnsetMdc(COUNTRY_CODE);
			assertUnsetMdc(BUSINESS_PROCESS);
		});
	}

	@ParameterizedTest
	@EnumSource
	void shouldRemoveEntriesFromMdcForNullSpan(AutoConfig autoConfig) {
		autoConfig.get().run((context) -> {
			Tracer tracer = tracer(context);
			Span span = createSpan(tracer);
			BaggageManager baggageManager = baggageManager(context);
			try (Tracer.SpanInScope scope = tracer.withSpan(span.start())) {
				assertMdcValue("traceId", span.context().traceId());
				try (BaggageInScope fo = baggageManager.createBaggageInScope(span.context(), COUNTRY_CODE, "FO")) {
					assertMdcValue(COUNTRY_CODE, "FO");
					try (Tracer.SpanInScope scope2 = tracer.withSpan(null)) {
						assertThatMdcContainsUnsetTraceId();
						assertUnsetMdc(COUNTRY_CODE);
					}
					assertMdcValue("traceId", span.context().traceId());
					assertMdcValue(COUNTRY_CODE, "FO");
				}
			}
			finally {
				span.end();
			}
			assertThatMdcContainsUnsetTraceId();
			assertUnsetMdc(COUNTRY_CODE);
		});
	}

	private Span createSpan(Tracer tracer) {
		return tracer.nextSpan().name("span");
	}

	private Tracer tracer(ApplicationContext context) {
		return context.getBean(Tracer.class);
	}

	private BaggageManager baggageManager(ApplicationContext context) {
		return context.getBean(BaggageManager.class);
	}

	private void assertThatMdcContainsUnsetTraceId() {
		assertThat(MDC.get("traceId")).isNull();
	}

	private void assertUnsetMdc(String key) {
		assertThat(MDC.get(key)).as("MDC[%s]", key).isNull();
	}

	private void assertMdcValue(String key, String expected) {
		assertThat(MDC.get(key)).as("MDC[%s]", key).isEqualTo(expected);
	}

	enum AutoConfig implements Supplier<ApplicationContextRunner> {

		BRAVE_DEFAULT {

			@Override
			public ApplicationContextRunner get() {
				return new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
					.withPropertyValues("management.tracing.baggage.remote-fields=x-vcap-request-id,country-code,bp",
							"management.tracing.baggage.correlation.fields=country-code,bp");
			}

		},

		BRAVE_W3C {

			@Override
			public ApplicationContextRunner get() {
				return new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
					.withPropertyValues("management.tracing.propagation.type=W3C",
							"management.tracing.baggage.remote-fields=x-vcap-request-id,country-code,bp",
							"management.tracing.baggage.correlation.fields=country-code,bp");
			}

		},

		BRAVE_B3 {

			@Override
			public ApplicationContextRunner get() {
				return new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
					.withPropertyValues("management.tracing.propagation.type=B3",
							"management.tracing.baggage.remote-fields=x-vcap-request-id,country-code,bp",
							"management.tracing.baggage.correlation.fields=country-code,bp");
			}

		},

		BRAVE_B3_MULTI {

			@Override
			public ApplicationContextRunner get() {
				return new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
					.withPropertyValues("management.tracing.propagation.type=B3_MULTI",
							"management.tracing.baggage.remote-fields=x-vcap-request-id,country-code,bp",
							"management.tracing.baggage.correlation.fields=country-code,bp");
			}

		},

		BRAVE_LOCAL_FIELDS {

			@Override
			public ApplicationContextRunner get() {
				return new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(BraveAutoConfiguration.class))
					.withPropertyValues("management.tracing.baggage.local-fields=country-code,bp",
							"management.tracing.baggage.correlation.fields=country-code,bp");
			}

		}

	}

}
