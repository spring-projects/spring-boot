/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.prometheus;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import io.prometheus.client.exporter.common.TextFormat;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrometheusExemplarsAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class PrometheusExemplarsAutoConfigurationTests {

	private static final Pattern BUCKET_TRACE_INFO_PATTERN = Pattern.compile(
			"^test_observation_seconds_bucket\\{error=\"none\",le=\".+\"} 1.0 # \\{span_id=\"(\\p{XDigit}+)\",trace_id=\"(\\p{XDigit}+)\"} .+$");

	private static final Pattern COUNTER_TRACE_INFO_PATTERN = Pattern.compile(
			"^test_observation_seconds_count\\{error=\"none\"} 1.0 # \\{span_id=\"(\\p{XDigit}+)\",trace_id=\"(\\p{XDigit}+)\"} .+$");

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("management.tracing.sampling.probability=1.0",
				"management.metrics.distribution.percentiles-histogram.all=true")
		.with(MetricsRun.limitedTo(PrometheusMetricsExportAutoConfiguration.class))
		.withConfiguration(
				AutoConfigurations.of(PrometheusExemplarsAutoConfiguration.class, ObservationAutoConfiguration.class,
						BraveAutoConfiguration.class, MicrometerTracingAutoConfiguration.class));

	@Test
	void shouldNotSupplyBeansIfPrometheusSupportIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.prometheus.client.exemplars"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContextSupplier.class));
	}

	@Test
	void shouldNotSupplyBeansIfMicrometerTracingIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.tracing"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContextSupplier.class));
	}

	@Test
	void shouldSupplyCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(SpanContextSupplier.class)
				.getBean(SpanContextSupplier.class)
				.isSameAs(CustomConfiguration.SUPPLIER));
	}

	@Test
	void prometheusOpenMetricsOutputShouldContainExemplars() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(SpanContextSupplier.class);
			ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
			Observation.start("test.observation", observationRegistry).stop();
			PrometheusMeterRegistry prometheusMeterRegistry = context.getBean(PrometheusMeterRegistry.class);
			String openMetricsOutput = prometheusMeterRegistry.scrape(TextFormat.CONTENT_TYPE_OPENMETRICS_100);

			assertThat(openMetricsOutput).contains("test_observation_seconds_bucket");
			assertThat(openMetricsOutput).containsOnlyOnce("test_observation_seconds_count");
			assertThat(StringUtils.countOccurrencesOf(openMetricsOutput, "span_id")).isEqualTo(2);
			assertThat(StringUtils.countOccurrencesOf(openMetricsOutput, "trace_id")).isEqualTo(2);

			Optional<TraceInfo> bucketTraceInfo = openMetricsOutput.lines()
				.filter((line) -> line.contains("test_observation_seconds_bucket") && line.contains("span_id"))
				.map(BUCKET_TRACE_INFO_PATTERN::matcher)
				.flatMap(Matcher::results)
				.map((matchResult) -> new TraceInfo(matchResult.group(2), matchResult.group(1)))
				.findFirst();

			Optional<TraceInfo> counterTraceInfo = openMetricsOutput.lines()
				.filter((line) -> line.contains("test_observation_seconds_count") && line.contains("span_id"))
				.map(COUNTER_TRACE_INFO_PATTERN::matcher)
				.flatMap(Matcher::results)
				.map((matchResult) -> new TraceInfo(matchResult.group(2), matchResult.group(1)))
				.findFirst();

			assertThat(bucketTraceInfo).isNotEmpty().contains(counterTraceInfo.orElse(null));
		});
	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		static final SpanContextSupplier SUPPLIER = mock(SpanContextSupplier.class);

		@Bean
		SpanContextSupplier customSpanContextSupplier() {
			return SUPPLIER;
		}

	}

	private record TraceInfo(String traceId, String spanId) {
	}

}
