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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.registry.otlp.ExemplarContextProvider;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.micrometer.registry.otlp.OtlpMetricsSender;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.otlp.OtlpExemplarsAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OtlpExemplarsAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class OtlpExemplarsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("management.tracing.sampling.probability=1.0",
				"management.metrics.distribution.percentiles-histogram.all=true",
				"management.metrics.use-global-registry=false")
		.withConfiguration(
				AutoConfigurations.of(MetricsAutoConfiguration.class, OtlpMetricsExportAutoConfiguration.class,
						OtlpExemplarsAutoConfiguration.class, ObservationAutoConfiguration.class,
						BraveAutoConfiguration.class, MicrometerTracingAutoConfiguration.class));

	@Test
	void shouldNotSupplyBeansIfOtlpSupportIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.registry.otlp"))
			.run((context) -> assertThat(context).doesNotHaveBean(ExemplarContextProvider.class));
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerTracingIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.tracing"))
			.run((context) -> assertThat(context).doesNotHaveBean(ExemplarContextProvider.class));
	}

	@Test
	void shouldSupplyCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ExemplarContextProvider.class)
				.getBean(ExemplarContextProvider.class)
				.isSameAs(CustomConfiguration.CONTEXT_PROVIDER));
	}

	@Test
	void otlpOutputShouldContainExemplars() {
		this.contextRunner.withUserConfiguration(TracingConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ExemplarContextProvider.class);
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test.observation", observationRegistry).stop();
			OtlpMeterRegistry otlpMeterRegistry = context.getBean(OtlpMeterRegistry.class);
			TestOtlpMetricsSender metricsSender = context.getBean(TestOtlpMetricsSender.class);
			otlpMeterRegistry.close();
			assertThat(metricsSender.getOtlpRequest()).containsOnlyOnce("name: \"test.observation\"")
				.containsOnlyOnce("exemplars")
				.containsOnlyOnce("span_id")
				.containsOnlyOnce("trace_id");
		});
	}

	@Test
	void otlpOutputShouldContainExemplarsWhenIncludeIsAllAndSpanIsNotSampled() {
		this.contextRunner.withUserConfiguration(TracingConfiguration.class)
			.withPropertyValues("management.tracing.sampling.probability=0.0",
					"management.tracing.exemplars.include=all")
			.run((context) -> {
				assertThat(context).hasSingleBean(ExemplarContextProvider.class);
				ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
				Observation.start("test.observation", observationRegistry).stop();
				OtlpMeterRegistry otlpMeterRegistry = context.getBean(OtlpMeterRegistry.class);
				TestOtlpMetricsSender metricsSender = context.getBean(TestOtlpMetricsSender.class);
				otlpMeterRegistry.close();
				assertThat(metricsSender.getOtlpRequest()).containsOnlyOnce("name: \"test.observation\"")
					.containsOnlyOnce("exemplars")
					.containsOnlyOnce("span_id")
					.containsOnlyOnce("trace_id");
			});
	}

	@Test
	void otlpOutputShouldNotContainExemplarsWhenIncludeIsNone() {
		this.contextRunner.withUserConfiguration(TracingConfiguration.class)
			.withPropertyValues("management.tracing.exemplars.include=none")
			.run((context) -> {
				assertThat(context).hasSingleBean(ExemplarContextProvider.class);
				ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
				Observation.start("test.observation", observationRegistry).stop();
				OtlpMeterRegistry otlpMeterRegistry = context.getBean(OtlpMeterRegistry.class);
				TestOtlpMetricsSender metricsSender = context.getBean(TestOtlpMetricsSender.class);
				otlpMeterRegistry.close();
				assertThat(metricsSender.getOtlpRequest()).containsOnlyOnce("name: \"test.observation\"")
					.doesNotContain("exemplars")
					.doesNotContain("span_id")
					.doesNotContain("trace_id");
			});
	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomConfiguration {

		static final ExemplarContextProvider CONTEXT_PROVIDER = mock(ExemplarContextProvider.class);

		@Bean
		ExemplarContextProvider customContextProvider() {
			return CONTEXT_PROVIDER;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TracingConfiguration {

		@Bean
		TracingAwareMeterObservationHandler<Context> tracingAwareMeterObservationHandler(MeterRegistry meterRegistry,
				Tracer tracer) {
			DefaultMeterObservationHandler delegate = new DefaultMeterObservationHandler(meterRegistry);
			return new TracingAwareMeterObservationHandler<>(delegate, tracer);
		}

		@Bean
		TestOtlpMetricsSender testOtlpMetricsSender() {
			return new TestOtlpMetricsSender();
		}

	}

	static class TestOtlpMetricsSender implements OtlpMetricsSender {

		private String request = "";

		@Override
		public void send(Request request) throws Exception {
			this.request = request.toString();
		}

		String getOtlpRequest() {
			return this.request;
		}

	}

}
