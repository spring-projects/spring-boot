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

package org.springframework.boot.opentelemetry.autoconfigure;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link OpenTelemetryEnvironmentVariableEnvironmentPostProcessor}.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class OpenTelemetryEnvironmentVariableEnvironmentPostProcessorTests {

	@Test
	void shouldMapOtelSdkEnabled(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.enabled")).isNull();
		environment = runProcessor(Map.of("OTEL_SDK_DISABLED", "true"));
		assertThat(environment.getProperty("management.opentelemetry.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_SDK_DISABLED", "false"));
		assertThat(environment.getProperty("management.opentelemetry.enabled")).isEqualTo("true");
		environment = runProcessor(Map.of("OTEL_SDK_DISABLED", "invalid-value"));
		assertThat(environment.getProperty("management.opentelemetry.enabled")).isNull();
		assertThat(output)
			.contains("Invalid value for boolean environment variable 'OTEL_SDK_DISABLED': 'invalid-value'");
	}

	@Test
	void shouldMapOtelPropagators(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertHasNoList(environment, "management.tracing.propagation.type");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "none"));
		assertHasEmptyList(environment, "management.tracing.propagation.type");
		assertThat(environment.getProperty("management.tracing.baggage.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "tracecontext"));
		assertHasList(environment, "management.tracing.propagation.type", "W3C");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "b3"));
		assertHasList(environment, "management.tracing.propagation.type", "B3");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "b3multi"));
		assertHasList(environment, "management.tracing.propagation.type", "B3_MULTI");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "tracecontext,b3,b3multi"));
		assertHasList(environment, "management.tracing.propagation.type", "W3C", "B3", "B3_MULTI");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", " TraceContext , B3 , B3multi "));
		assertHasList(environment, "management.tracing.propagation.type", "W3C", "B3", "B3_MULTI");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "none,tracecontext,b3,b3multi"));
		assertHasEmptyList(environment, "management.tracing.propagation.type");
		assertThat(environment.getProperty("management.tracing.baggage.enabled")).isEqualTo("false");
		assertThat(output).contains(
				"Environment variable 'OTEL_PROPAGATORS' contains 'none', but also contains more elements: 'none,tracecontext,b3,b3multi'");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "baggage"));
		assertHasEmptyList(environment, "management.tracing.propagation.type");
		assertThat(environment.getProperty("management.tracing.baggage.enabled")).isEqualTo("true");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "jaeger"));
		assertHasEmptyList(environment, "management.tracing.propagation.type");
		assertThat(output).contains("Unsupported propagator 'jaeger' in environment variable 'OTEL_PROPAGATORS'");
		environment = runProcessor(Map.of("OTEL_PROPAGATORS", "tracecontext,xray,b3"));
		assertHasList(environment, "management.tracing.propagation.type", "W3C", "B3");
		assertThat(output).contains("Unsupported propagator 'xray' in environment variable 'OTEL_PROPAGATORS'");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsHeaders() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertHasNoMap(environment, "management.otlp.metrics.export.headers");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_HEADERS", "a=b,c=d"));
		assertHasMap(environment, "management.otlp.metrics.export.headers", Map.of("a", "b", "c", "d"));
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsHeadersFallback() {
		Environment environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_HEADERS", "x=y"));
		assertHasMap(environment, "management.otlp.metrics.export.headers", Map.of("x", "y"));
	}

	@Test
	void shouldMapOtelTracesSampler(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler")).isNull();
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "always_on"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler")).isEqualTo("ALWAYS_ON");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "always_off"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler")).isEqualTo("ALWAYS_OFF");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "traceidratio"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler")).isEqualTo("TRACE_ID_RATIO");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "parentbased_always_on"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler"))
			.isEqualTo("PARENT_BASED_ALWAYS_ON");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "parentbased_always_off"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler"))
			.isEqualTo("PARENT_BASED_ALWAYS_OFF");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "parentbased_traceidratio"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler"))
			.isEqualTo("PARENT_BASED_TRACE_ID_RATIO");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "invalid"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler")).isNull();
		assertThat(output).contains("Invalid value for environment variable 'OTEL_TRACES_SAMPLER': 'invalid'");
	}

	@Test
	void shouldMapOtelTracesSamplerArg(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.tracing.sampling.probability")).isNull();
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "traceidratio", "OTEL_TRACES_SAMPLER_ARG", "0.5"));
		assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("0.5");
		environment = runProcessor(
				Map.of("OTEL_TRACES_SAMPLER", "parentbased_traceidratio", "OTEL_TRACES_SAMPLER_ARG", "0.25"));
		assertThat(environment.getProperty("management.tracing.sampling.probability")).isEqualTo("0.25");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER_ARG", "0.5"));
		assertThat(environment.getProperty("management.tracing.sampling.probability")).isNull();
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "always_on", "OTEL_TRACES_SAMPLER_ARG", "0.5"));
		assertThat(environment.getProperty("management.tracing.sampling.probability")).isNull();
		assertThat(output)
			.contains("Unsupported environment variable 'OTEL_TRACES_SAMPLER_ARG' for sampler 'always_on'");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "traceidratio", "OTEL_TRACES_SAMPLER_ARG", "2.0"));
		assertThat(environment.getProperty("management.tracing.sampling.probability")).isNull();
		assertThat(output).contains(
				"Invalid value for environment variable 'OTEL_TRACES_SAMPLER_ARG'. Must be between 0.0 and 1.0: '2.0'");
		environment = runProcessor(
				Map.of("OTEL_TRACES_SAMPLER", "traceidratio", "OTEL_TRACES_SAMPLER_ARG", "not-a-number"));
		assertThat(environment.getProperty("management.tracing.sampling.probability")).isNull();
		assertThat(output).contains("Invalid value for environment variable 'OTEL_TRACES_SAMPLER_ARG': 'not-a-number'");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsEndpoint() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.url")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT", "http://metrics:4318/v1/metrics"));
		assertThat(environment.getProperty("management.otlp.metrics.export.url"))
			.isEqualTo("http://metrics:4318/v1/metrics");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318"));
		assertThat(environment.getProperty("management.otlp.metrics.export.url"))
			.isEqualTo("http://collector:4318/v1/metrics");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsEndpointSpecificOverridesFallback() {
		Environment environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT",
				"http://metrics:4318/v1/metrics", "OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318"));
		assertThat(environment.getProperty("management.otlp.metrics.export.url"))
			.isEqualTo("http://metrics:4318/v1/metrics");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsTemporalityPreference(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.aggregation-temporality")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE", "cumulative"));
		assertThat(environment.getProperty("management.otlp.metrics.export.aggregation-temporality"))
			.isEqualTo("CUMULATIVE");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE", "delta"));
		assertThat(environment.getProperty("management.otlp.metrics.export.aggregation-temporality"))
			.isEqualTo("DELTA");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE", "invalid"));
		assertThat(environment.getProperty("management.otlp.metrics.export.aggregation-temporality")).isNull();
		assertThat(output).contains(
				"Invalid value for environment variable 'OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsDefaultHistogramAggregation(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.histogram-flavor")).isNull();
		environment = runProcessor(
				Map.of("OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION", "explicit_bucket_histogram"));
		assertThat(environment.getProperty("management.otlp.metrics.export.histogram-flavor"))
			.isEqualTo("EXPLICIT_BUCKET_HISTOGRAM");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION",
				"base2_exponential_bucket_histogram"));
		assertThat(environment.getProperty("management.otlp.metrics.export.histogram-flavor"))
			.isEqualTo("BASE2_EXPONENTIAL_BUCKET_HISTOGRAM");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION", "invalid"));
		assertThat(environment.getProperty("management.otlp.metrics.export.histogram-flavor")).isNull();
		assertThat(output).contains(
				"Invalid value for environment variable 'OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsCompression(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.compression-mode")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_COMPRESSION", "gzip"));
		assertThat(environment.getProperty("management.otlp.metrics.export.compression-mode")).isEqualTo("GZIP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_COMPRESSION", "none"));
		assertThat(environment.getProperty("management.otlp.metrics.export.compression-mode")).isEqualTo("NONE");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_COMPRESSION", "gzip"));
		assertThat(environment.getProperty("management.otlp.metrics.export.compression-mode")).isEqualTo("GZIP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_COMPRESSION", "invalid"));
		assertThat(environment.getProperty("management.otlp.metrics.export.compression-mode")).isNull();
		assertThat(output)
			.contains("Invalid value for environment variable 'OTEL_EXPORTER_OTLP_METRICS_COMPRESSION': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsTimeout() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.read-timeout")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_TIMEOUT", "5000"));
		assertThat(environment.getProperty("management.otlp.metrics.export.read-timeout")).isEqualTo("PT5S");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT", "3000"));
		assertThat(environment.getProperty("management.otlp.metrics.export.read-timeout")).isEqualTo("PT3S");
	}

	@Test
	void shouldMapOtelMetricExportInterval() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.step")).isNull();
		environment = runProcessor(Map.of("OTEL_METRIC_EXPORT_INTERVAL", "60000"));
		assertThat(environment.getProperty("management.otlp.metrics.export.step")).isEqualTo("PT1M");
	}

	@Test
	void shouldMapOtelMetricsExporter(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.enabled")).isNull();
		environment = runProcessor(Map.of("OTEL_METRICS_EXPORTER", "otlp"));
		assertThat(environment.getProperty("management.otlp.metrics.export.enabled")).isEqualTo("true");
		environment = runProcessor(Map.of("OTEL_METRICS_EXPORTER", "none"));
		assertThat(environment.getProperty("management.otlp.metrics.export.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_METRICS_EXPORTER", "prometheus"));
		assertThat(environment.getProperty("management.otlp.metrics.export.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_METRICS_EXPORTER", "none,otlp"));
		assertThat(environment.getProperty("management.otlp.metrics.export.enabled")).isEqualTo("false");
		assertThat(output).contains(
				"Environment variable 'OTEL_METRICS_EXPORTER' contains 'none', but also contains more elements: 'none,otlp'");
		environment = runProcessor(Map.of("OTEL_METRICS_EXPORTER", " OTLP , prometheus "));
		assertThat(environment.getProperty("management.otlp.metrics.export.enabled")).isEqualTo("true");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsSslBundle() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.otlp.metrics.export.ssl.bundle")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_CERTIFICATE", "/path/to/cert.pem"));
		assertThat(environment.getProperty("management.otlp.metrics.export.ssl.bundle"))
			.isEqualTo("opentelemetry-metrics");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.truststore.certificate"))
			.isEqualTo("file:/path/to/cert.pem");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_CLIENT_KEY", "/path/to/key.pem",
				"OTEL_EXPORTER_OTLP_METRICS_CLIENT_CERTIFICATE", "/path/to/client-cert.pem"));
		assertThat(environment.getProperty("management.otlp.metrics.export.ssl.bundle"))
			.isEqualTo("opentelemetry-metrics");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.keystore.private-key"))
			.isEqualTo("file:/path/to/key.pem");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.keystore.certificate"))
			.isEqualTo("file:/path/to/client-cert.pem");
	}

	@Test
	void shouldMapOtelExporterOtlpMetricsSslBundleFallback() {
		Environment environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CERTIFICATE", "/path/to/cert.pem"));
		assertThat(environment.getProperty("management.otlp.metrics.export.ssl.bundle"))
			.isEqualTo("opentelemetry-metrics");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.truststore.certificate"))
			.isEqualTo("file:/path/to/cert.pem");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CLIENT_KEY", "/path/to/key.pem"));
		assertThat(environment.getProperty("management.otlp.metrics.export.ssl.bundle"))
			.isEqualTo("opentelemetry-metrics");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.keystore.private-key"))
			.isEqualTo("file:/path/to/key.pem");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE", "/path/to/client-cert.pem"));
		assertThat(environment.getProperty("management.otlp.metrics.export.ssl.bundle"))
			.isEqualTo("opentelemetry-metrics");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.keystore.certificate"))
			.isEqualTo("file:/path/to/client-cert.pem");
	}

	@Test
	void shouldMapOtelExporterOtlpTracesEndpoint() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "http://traces:4318/v1/traces"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
			.isEqualTo("http://traces:4318/v1/traces");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
			.isEqualTo("http://collector:4318/v1/traces");
	}

	@Test
	void shouldMapOtelExporterOtlpTracesCompression(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.compression")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_COMPRESSION", "gzip"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.compression"))
			.isEqualTo("GZIP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_COMPRESSION", "none"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.compression"))
			.isEqualTo("NONE");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_COMPRESSION", "gzip"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.compression"))
			.isEqualTo("GZIP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_COMPRESSION", "invalid"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.compression")).isNull();
		assertThat(output)
			.contains("Invalid value for environment variable 'OTEL_EXPORTER_OTLP_TRACES_COMPRESSION': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpTracesTimeout() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.timeout")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_TIMEOUT", "5000"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.timeout")).isEqualTo("PT5S");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT", "3000"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.timeout")).isEqualTo("PT3S");
	}

	@Test
	void shouldMapOtelExporterOtlpTracesProtocol(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", "grpc"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isEqualTo("GRPC");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", "http/protobuf"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isEqualTo("HTTP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isEqualTo("GRPC");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", "invalid"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isNull();
		assertThat(output)
			.contains("Invalid value for environment variable 'OTEL_EXPORTER_OTLP_TRACES_PROTOCOL': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpTracesHeaders() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertHasNoMap(environment, "management.opentelemetry.tracing.export.otlp.headers");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_HEADERS", "a=b,c=d"));
		assertHasMap(environment, "management.opentelemetry.tracing.export.otlp.headers", Map.of("a", "b", "c", "d"));
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_HEADERS", "x=y"));
		assertHasMap(environment, "management.opentelemetry.tracing.export.otlp.headers", Map.of("x", "y"));
	}

	@Test
	void shouldMapOtelBspScheduleDelay() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.schedule-delay")).isNull();
		environment = runProcessor(Map.of("OTEL_BSP_SCHEDULE_DELAY", "5000"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.schedule-delay")).isEqualTo("PT5S");
	}

	@Test
	void shouldMapOtelBspExportTimeout() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.timeout")).isNull();
		environment = runProcessor(Map.of("OTEL_BSP_EXPORT_TIMEOUT", "30000"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.timeout")).isEqualTo("PT30S");
	}

	@Test
	void shouldMapOtelBspMaxQueueSize() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.max-queue-size")).isNull();
		environment = runProcessor(Map.of("OTEL_BSP_MAX_QUEUE_SIZE", "2048"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.max-queue-size")).isEqualTo("2048");
	}

	@Test
	void shouldMapOtelBspMaxExportBatchSize() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.max-batch-size")).isNull();
		environment = runProcessor(Map.of("OTEL_BSP_MAX_EXPORT_BATCH_SIZE", "512"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.max-batch-size")).isEqualTo("512");
	}

	@Test
	void shouldMapOtelTracesExporter(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.tracing.export.otlp.enabled")).isNull();
		environment = runProcessor(Map.of("OTEL_TRACES_EXPORTER", "otlp"));
		assertThat(environment.getProperty("management.tracing.export.otlp.enabled")).isEqualTo("true");
		environment = runProcessor(Map.of("OTEL_TRACES_EXPORTER", "none"));
		assertThat(environment.getProperty("management.tracing.export.otlp.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_TRACES_EXPORTER", "zipkin"));
		assertThat(environment.getProperty("management.tracing.export.otlp.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_TRACES_EXPORTER", "none,otlp"));
		assertThat(environment.getProperty("management.tracing.export.otlp.enabled")).isEqualTo("false");
		assertThat(output).contains(
				"Environment variable 'OTEL_TRACES_EXPORTER' contains 'none', but also contains more elements: 'none,otlp'");
		environment = runProcessor(Map.of("OTEL_TRACES_EXPORTER", " OTLP , zipkin "));
		assertThat(environment.getProperty("management.tracing.export.otlp.enabled")).isEqualTo("true");
	}

	@Test
	void shouldMapOtelSpanAttributeValueLengthLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attribute-value-length"))
			.isNull();
		environment = runProcessor(Map.of("OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT", "4096"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attribute-value-length"))
			.isEqualTo("4096");
		environment = runProcessor(Map.of("OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT", "2048"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attribute-value-length"))
			.isEqualTo("2048");
	}

	@Test
	void shouldMapOtelSpanAttributeCountLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes")).isNull();
		environment = runProcessor(Map.of("OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT", "128"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes")).isEqualTo("128");
		environment = runProcessor(Map.of("OTEL_ATTRIBUTE_COUNT_LIMIT", "64"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes")).isEqualTo("64");
	}

	@Test
	void shouldMapOtelSpanEventCountLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-events")).isNull();
		environment = runProcessor(Map.of("OTEL_SPAN_EVENT_COUNT_LIMIT", "128"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-events")).isEqualTo("128");
	}

	@Test
	void shouldMapOtelSpanLinkCountLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-links")).isNull();
		environment = runProcessor(Map.of("OTEL_SPAN_LINK_COUNT_LIMIT", "128"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-links")).isEqualTo("128");
	}

	@Test
	void shouldMapOtelEventAttributeCountLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes-per-event"))
			.isNull();
		environment = runProcessor(Map.of("OTEL_EVENT_ATTRIBUTE_COUNT_LIMIT", "128"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes-per-event"))
			.isEqualTo("128");
	}

	@Test
	void shouldMapOtelLinkAttributeCountLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes-per-link")).isNull();
		environment = runProcessor(Map.of("OTEL_LINK_ATTRIBUTE_COUNT_LIMIT", "128"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes-per-link"))
			.isEqualTo("128");
	}

	@Test
	void shouldMapOtelMetricsExemplarFilter(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.tracing.exemplars.include")).isNull();
		environment = runProcessor(Map.of("OTEL_METRICS_EXEMPLAR_FILTER", "always_on"));
		assertThat(environment.getProperty("management.tracing.exemplars.include")).isEqualTo("ALL");
		environment = runProcessor(Map.of("OTEL_METRICS_EXEMPLAR_FILTER", "always_off"));
		assertThat(environment.getProperty("management.tracing.exemplars.include")).isEqualTo("NONE");
		environment = runProcessor(Map.of("OTEL_METRICS_EXEMPLAR_FILTER", "trace_based"));
		assertThat(environment.getProperty("management.tracing.exemplars.include")).isEqualTo("SAMPLED_TRACES");
		environment = runProcessor(Map.of("OTEL_METRICS_EXEMPLAR_FILTER", "invalid"));
		assertThat(environment.getProperty("management.tracing.exemplars.include")).isNull();
		assertThat(output).contains("Invalid value for environment variable 'OTEL_METRICS_EXEMPLAR_FILTER': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpTracesSslBundle() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.ssl.bundle")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_CERTIFICATE", "/path/to/cert.pem"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-tracing");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-tracing.truststore.certificate"))
			.isEqualTo("file:/path/to/cert.pem");
	}

	@Test
	void shouldMapOtelExporterOtlpTracesSslBundleFallback() {
		Environment environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CERTIFICATE", "/path/to/cert.pem"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-tracing");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-tracing.truststore.certificate"))
			.isEqualTo("file:/path/to/cert.pem");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CLIENT_KEY", "/path/to/key.pem"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-tracing");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-tracing.keystore.private-key"))
			.isEqualTo("file:/path/to/key.pem");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE", "/path/to/client-cert.pem"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-tracing");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-tracing.keystore.certificate"))
			.isEqualTo("file:/path/to/client-cert.pem");
	}

	@Test
	void shouldMapOtelExporterOtlpLogsEndpoint() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.endpoint")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "http://logs:4318/v1/logs"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.endpoint"))
			.isEqualTo("http://logs:4318/v1/logs");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.endpoint"))
			.isEqualTo("http://collector:4318/v1/logs");
	}

	@Test
	void shouldMapOtelExporterOtlpLogsCompression(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.compression")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_COMPRESSION", "gzip"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.compression"))
			.isEqualTo("GZIP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_COMPRESSION", "none"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.compression"))
			.isEqualTo("NONE");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_COMPRESSION", "gzip"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.compression"))
			.isEqualTo("GZIP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_COMPRESSION", "invalid"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.compression")).isNull();
		assertThat(output)
			.contains("Invalid value for environment variable 'OTEL_EXPORTER_OTLP_LOGS_COMPRESSION': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpLogsTimeout() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.timeout")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_TIMEOUT", "5000"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.timeout")).isEqualTo("PT5S");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TIMEOUT", "3000"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.timeout")).isEqualTo("PT3S");
	}

	@Test
	void shouldMapOtelExporterOtlpLogsProtocol(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.transport")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_PROTOCOL", "grpc"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.transport")).isEqualTo("GRPC");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_PROTOCOL", "http/protobuf"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.transport")).isEqualTo("HTTP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.transport")).isEqualTo("GRPC");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_PROTOCOL", "invalid"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.transport")).isNull();
		assertThat(output)
			.contains("Invalid value for environment variable 'OTEL_EXPORTER_OTLP_LOGS_PROTOCOL': 'invalid'");
	}

	@Test
	void shouldMapOtelExporterOtlpLogsHeaders() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertHasNoMap(environment, "management.opentelemetry.logging.export.otlp.headers");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_HEADERS", "a=b,c=d"));
		assertHasMap(environment, "management.opentelemetry.logging.export.otlp.headers", Map.of("a", "b", "c", "d"));
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_HEADERS", "x=y"));
		assertHasMap(environment, "management.opentelemetry.logging.export.otlp.headers", Map.of("x", "y"));
	}

	@Test
	void shouldMapOtelBlrpScheduleDelay() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.schedule-delay")).isNull();
		environment = runProcessor(Map.of("OTEL_BLRP_SCHEDULE_DELAY", "5000"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.schedule-delay")).isEqualTo("PT5S");
	}

	@Test
	void shouldMapOtelBlrpExportTimeout() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.timeout")).isNull();
		environment = runProcessor(Map.of("OTEL_BLRP_EXPORT_TIMEOUT", "30000"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.timeout")).isEqualTo("PT30S");
	}

	@Test
	void shouldMapOtelBlrpMaxQueueSize() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.max-queue-size")).isNull();
		environment = runProcessor(Map.of("OTEL_BLRP_MAX_QUEUE_SIZE", "2048"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.max-queue-size")).isEqualTo("2048");
	}

	@Test
	void shouldMapOtelBlrpMaxExportBatchSize() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.max-batch-size")).isNull();
		environment = runProcessor(Map.of("OTEL_BLRP_MAX_EXPORT_BATCH_SIZE", "512"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.max-batch-size")).isEqualTo("512");
	}

	@Test
	void shouldMapOtelLogsExporter(CapturedOutput output) {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.logging.export.otlp.enabled")).isNull();
		environment = runProcessor(Map.of("OTEL_LOGS_EXPORTER", "otlp"));
		assertThat(environment.getProperty("management.logging.export.otlp.enabled")).isEqualTo("true");
		environment = runProcessor(Map.of("OTEL_LOGS_EXPORTER", "none"));
		assertThat(environment.getProperty("management.logging.export.otlp.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_LOGS_EXPORTER", "console"));
		assertThat(environment.getProperty("management.logging.export.otlp.enabled")).isEqualTo("false");
		environment = runProcessor(Map.of("OTEL_LOGS_EXPORTER", "none,otlp"));
		assertThat(environment.getProperty("management.logging.export.otlp.enabled")).isEqualTo("false");
		assertThat(output).contains(
				"Environment variable 'OTEL_LOGS_EXPORTER' contains 'none', but also contains more elements: 'none,otlp'");
		environment = runProcessor(Map.of("OTEL_LOGS_EXPORTER", " OTLP , console "));
		assertThat(environment.getProperty("management.logging.export.otlp.enabled")).isEqualTo("true");
	}

	@Test
	void shouldMapOtelLogrecordAttributeValueLengthLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attribute-value-length"))
			.isNull();
		environment = runProcessor(Map.of("OTEL_LOGRECORD_ATTRIBUTE_VALUE_LENGTH_LIMIT", "4096"));
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attribute-value-length"))
			.isEqualTo("4096");
		environment = runProcessor(Map.of("OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT", "2048"));
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attribute-value-length"))
			.isEqualTo("2048");
	}

	@Test
	void shouldMapOtelLogrecordAttributeCountLimit() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attributes")).isNull();
		environment = runProcessor(Map.of("OTEL_LOGRECORD_ATTRIBUTE_COUNT_LIMIT", "128"));
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attributes")).isEqualTo("128");
		environment = runProcessor(Map.of("OTEL_ATTRIBUTE_COUNT_LIMIT", "64"));
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attributes")).isEqualTo("64");
	}

	@Test
	void shouldMapOtelExporterOtlpLogsSslBundle() {
		Environment environment = runProcessor(Collections.emptyMap());
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.ssl.bundle")).isNull();
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_CERTIFICATE", "/path/to/cert.pem"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-logging");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-logging.truststore.certificate"))
			.isEqualTo("file:/path/to/cert.pem");
	}

	@Test
	void shouldMapOtelExporterOtlpLogsSslBundleFallback() {
		Environment environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CERTIFICATE", "/path/to/cert.pem"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-logging");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-logging.truststore.certificate"))
			.isEqualTo("file:/path/to/cert.pem");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CLIENT_KEY", "/path/to/key.pem"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-logging");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-logging.keystore.private-key"))
			.isEqualTo("file:/path/to/key.pem");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE", "/path/to/client-cert.pem"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.ssl.bundle"))
			.isEqualTo("opentelemetry-logging");
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-logging.keystore.certificate"))
			.isEqualTo("file:/path/to/client-cert.pem");
	}

	@Test
	void enumValuesShouldBeCaseInsensitive() {
		Environment environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "ALWAYS_ON"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler")).isEqualTo("ALWAYS_ON");
		environment = runProcessor(Map.of("OTEL_TRACES_SAMPLER", "Always_On"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.sampler")).isEqualTo("ALWAYS_ON");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_COMPRESSION", "GZIP"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.compression"))
			.isEqualTo("GZIP");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", "GRPC"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isEqualTo("GRPC");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", "HTTP/PROTOBUF"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isEqualTo("HTTP");
		environment = runProcessor(Map.of("OTEL_METRICS_EXEMPLAR_FILTER", "TRACE_BASED"));
		assertThat(environment.getProperty("management.tracing.exemplars.include")).isEqualTo("SAMPLED_TRACES");
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE", "CUMULATIVE"));
		assertThat(environment.getProperty("management.otlp.metrics.export.aggregation-temporality"))
			.isEqualTo("CUMULATIVE");
		environment = runProcessor(
				Map.of("OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION", "EXPLICIT_BUCKET_HISTOGRAM"));
		assertThat(environment.getProperty("management.otlp.metrics.export.histogram-flavor"))
			.isEqualTo("EXPLICIT_BUCKET_HISTOGRAM");
	}

	@Test
	void fallbackEndpointWithTrailingSlashShouldAppendPathCorrectly() {
		Environment environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318/"));
		assertThat(environment.getProperty("management.otlp.metrics.export.url"))
			.isEqualTo("http://collector:4318/v1/metrics");
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
			.isEqualTo("http://collector:4318/v1/traces");
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.endpoint"))
			.isEqualTo("http://collector:4318/v1/logs");
	}

	@Test
	void timeoutZeroShouldBeInterpretedAsInfinite() {
		Environment environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_TRACES_TIMEOUT", "0"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.timeout"))
			.isEqualTo(Duration.ofSeconds(Long.MAX_VALUE).toString());
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_LOGS_TIMEOUT", "0"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.timeout"))
			.isEqualTo(Duration.ofSeconds(Long.MAX_VALUE).toString());
		environment = runProcessor(Map.of("OTEL_EXPORTER_OTLP_METRICS_TIMEOUT", "0"));
		assertThat(environment.getProperty("management.otlp.metrics.export.read-timeout"))
			.isEqualTo(Duration.ofSeconds(Long.MAX_VALUE).toString());
		environment = runProcessor(Map.of("OTEL_BSP_EXPORT_TIMEOUT", "0"));
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.timeout"))
			.isEqualTo(Duration.ofSeconds(Long.MAX_VALUE).toString());
		environment = runProcessor(Map.of("OTEL_BLRP_EXPORT_TIMEOUT", "0"));
		assertThat(environment.getProperty("management.opentelemetry.logging.export.timeout"))
			.isEqualTo(Duration.ofSeconds(Long.MAX_VALUE).toString());
	}

	@Test
	void shouldNotAddPropertySourceWhenNoEnvironmentVariablesAreSet() {
		ConfigurableEnvironment environment = new StandardEnvironment();
		int sourceCountBefore = environment.getPropertySources().size();
		DeferredLogs logFactory = new DeferredLogs();
		OpenTelemetryEnvironmentVariableEnvironmentPostProcessor processor = new OpenTelemetryEnvironmentVariableEnvironmentPostProcessor(
				logFactory, new OpenTelemetryEnvironmentVariables(logFactory, (name) -> null));
		processor.postProcessEnvironment(environment, new SpringApplication());
		assertThat(environment.getPropertySources()).hasSize(sourceCountBefore);
	}

	@Test
	void shouldNotMapWhenDisabledViaProperty() {
		DeferredLogs logFactory = new DeferredLogs();
		Map<String, String> envVars = Map.of("OTEL_SDK_DISABLED", "true");
		OpenTelemetryEnvironmentVariableEnvironmentPostProcessor processor = new OpenTelemetryEnvironmentVariableEnvironmentPostProcessor(
				logFactory, new OpenTelemetryEnvironmentVariables(logFactory, envVars::get));
		ConfigurableEnvironment environment = new StandardEnvironment();
		environment.getPropertySources()
			.addFirst(new org.springframework.core.env.MapPropertySource("test",
					Map.of("management.opentelemetry.map-environment-variables", "false")));
		int sourceCountBefore = environment.getPropertySources().size();
		processor.postProcessEnvironment(environment, new SpringApplication());
		assertThat(environment.getPropertySources()).hasSize(sourceCountBefore);
		assertThat(environment.getProperty("management.opentelemetry.enabled")).isNull();
	}

	@Test
	void specificMetricsCompressionShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_METRICS_COMPRESSION", "gzip");
		env.put("OTEL_EXPORTER_OTLP_COMPRESSION", "none");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.otlp.metrics.export.compression-mode")).isEqualTo("GZIP");
	}

	@Test
	void specificMetricsTimeoutShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_METRICS_TIMEOUT", "5000");
		env.put("OTEL_EXPORTER_OTLP_TIMEOUT", "3000");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.otlp.metrics.export.read-timeout")).isEqualTo("PT5S");
	}

	@Test
	void specificMetricsHeadersShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_METRICS_HEADERS", "a=b");
		env.put("OTEL_EXPORTER_OTLP_HEADERS", "x=y");
		Environment environment = runProcessor(env);
		assertHasMap(environment, "management.otlp.metrics.export.headers", Map.of("a", "b"));
	}

	@Test
	void specificMetricsCertificateShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_METRICS_CERTIFICATE", "/specific/cert.pem");
		env.put("OTEL_EXPORTER_OTLP_CERTIFICATE", "/generic/cert.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.truststore.certificate"))
			.isEqualTo("file:/specific/cert.pem");
	}

	@Test
	void specificMetricsClientKeyShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_METRICS_CLIENT_KEY", "/specific/key.pem");
		env.put("OTEL_EXPORTER_OTLP_CLIENT_KEY", "/generic/key.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.keystore.private-key"))
			.isEqualTo("file:/specific/key.pem");
	}

	@Test
	void specificMetricsClientCertificateShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_METRICS_CLIENT_CERTIFICATE", "/specific/client.pem");
		env.put("OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE", "/generic/client.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-metrics.keystore.certificate"))
			.isEqualTo("file:/specific/client.pem");
	}

	@Test
	void specificTracesEndpointShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT", "http://traces:4318/v1/traces");
		env.put("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
			.isEqualTo("http://traces:4318/v1/traces");
	}

	@Test
	void specificTracesCompressionShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_COMPRESSION", "gzip");
		env.put("OTEL_EXPORTER_OTLP_COMPRESSION", "none");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.compression"))
			.isEqualTo("GZIP");
	}

	@Test
	void specificTracesTimeoutShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_TIMEOUT", "5000");
		env.put("OTEL_EXPORTER_OTLP_TIMEOUT", "3000");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.timeout")).isEqualTo("PT5S");
	}

	@Test
	void specificTracesProtocolShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_PROTOCOL", "http/protobuf");
		env.put("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.transport")).isEqualTo("HTTP");
	}

	@Test
	void specificTracesHeadersShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_HEADERS", "a=b");
		env.put("OTEL_EXPORTER_OTLP_HEADERS", "x=y");
		Environment environment = runProcessor(env);
		assertHasMap(environment, "management.opentelemetry.tracing.export.otlp.headers", Map.of("a", "b"));
	}

	@Test
	void specificTracesAttributeValueLengthLimitShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT", "4096");
		env.put("OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT", "2048");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attribute-value-length"))
			.isEqualTo("4096");
	}

	@Test
	void specificTracesAttributeCountLimitShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT", "128");
		env.put("OTEL_ATTRIBUTE_COUNT_LIMIT", "64");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.tracing.limits.max-attributes")).isEqualTo("128");
	}

	@Test
	void specificTracesCertificateShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_CERTIFICATE", "/specific/cert.pem");
		env.put("OTEL_EXPORTER_OTLP_CERTIFICATE", "/generic/cert.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-tracing.truststore.certificate"))
			.isEqualTo("file:/specific/cert.pem");
	}

	@Test
	void specificTracesClientKeyShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_CLIENT_KEY", "/specific/key.pem");
		env.put("OTEL_EXPORTER_OTLP_CLIENT_KEY", "/generic/key.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-tracing.keystore.private-key"))
			.isEqualTo("file:/specific/key.pem");
	}

	@Test
	void specificTracesClientCertificateShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_TRACES_CLIENT_CERTIFICATE", "/specific/client.pem");
		env.put("OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE", "/generic/client.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-tracing.keystore.certificate"))
			.isEqualTo("file:/specific/client.pem");
	}

	@Test
	void specificLogsEndpointShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_ENDPOINT", "http://logs:4318/v1/logs");
		env.put("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4318");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.endpoint"))
			.isEqualTo("http://logs:4318/v1/logs");
	}

	@Test
	void specificLogsCompressionShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_COMPRESSION", "gzip");
		env.put("OTEL_EXPORTER_OTLP_COMPRESSION", "none");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.compression"))
			.isEqualTo("GZIP");
	}

	@Test
	void specificLogsTimeoutShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_TIMEOUT", "5000");
		env.put("OTEL_EXPORTER_OTLP_TIMEOUT", "3000");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.timeout")).isEqualTo("PT5S");
	}

	@Test
	void specificLogsProtocolShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_PROTOCOL", "http/protobuf");
		env.put("OTEL_EXPORTER_OTLP_PROTOCOL", "grpc");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.transport")).isEqualTo("HTTP");
	}

	@Test
	void specificLogsHeadersShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_HEADERS", "a=b");
		env.put("OTEL_EXPORTER_OTLP_HEADERS", "x=y");
		Environment environment = runProcessor(env);
		assertHasMap(environment, "management.opentelemetry.logging.export.otlp.headers", Map.of("a", "b"));
	}

	@Test
	void specificLogsAttributeValueLengthLimitShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_LOGRECORD_ATTRIBUTE_VALUE_LENGTH_LIMIT", "4096");
		env.put("OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT", "2048");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attribute-value-length"))
			.isEqualTo("4096");
	}

	@Test
	void specificLogsAttributeCountLimitShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_LOGRECORD_ATTRIBUTE_COUNT_LIMIT", "128");
		env.put("OTEL_ATTRIBUTE_COUNT_LIMIT", "64");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("management.opentelemetry.logging.limits.max-attributes")).isEqualTo("128");
	}

	@Test
	void specificLogsCertificateShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_CERTIFICATE", "/specific/cert.pem");
		env.put("OTEL_EXPORTER_OTLP_CERTIFICATE", "/generic/cert.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-logging.truststore.certificate"))
			.isEqualTo("file:/specific/cert.pem");
	}

	@Test
	void specificLogsClientKeyShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_CLIENT_KEY", "/specific/key.pem");
		env.put("OTEL_EXPORTER_OTLP_CLIENT_KEY", "/generic/key.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-logging.keystore.private-key"))
			.isEqualTo("file:/specific/key.pem");
	}

	@Test
	void specificLogsClientCertificateShouldOverrideFallback() {
		Map<String, String> env = new HashMap<>();
		env.put("OTEL_EXPORTER_OTLP_LOGS_CLIENT_CERTIFICATE", "/specific/client.pem");
		env.put("OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE", "/generic/client.pem");
		Environment environment = runProcessor(env);
		assertThat(environment.getProperty("spring.ssl.bundle.pem.opentelemetry-logging.keystore.certificate"))
			.isEqualTo("file:/specific/client.pem");
	}

	private Environment runProcessor(Map<String, String> environmentVariables) {
		DeferredLogs logFactory = new DeferredLogs();
		OpenTelemetryEnvironmentVariableEnvironmentPostProcessor processor = new OpenTelemetryEnvironmentVariableEnvironmentPostProcessor(
				logFactory, new OpenTelemetryEnvironmentVariables(logFactory, environmentVariables::get));
		ConfigurableEnvironment configurableEnvironment = new StandardEnvironment();
		processor.postProcessEnvironment(configurableEnvironment, new SpringApplication());
		logFactory.switchOverAll();
		return configurableEnvironment;
	}

	private void assertHasNoMap(Environment environment, String key) {
		enumerateAllProperties(environment, (name) -> assertThat(name).doesNotStartWith(key + "["));
	}

	private void assertHasMap(Environment environment, String key, Map<String, String> map) {
		Set<String> ignoredProperties = new HashSet<>();
		for (Entry<String, String> entry : map.entrySet()) {
			String property = "%s[%s]".formatted(key, entry.getKey());
			assertThat(environment.getProperty(property)).isEqualTo(entry.getValue());
			ignoredProperties.add(property);
		}
		enumerateAllProperties(environment, ignoredProperties, (name) -> assertThat(name).doesNotStartWith(key + "["));
	}

	private void assertHasList(Environment environment, String key, String... values) {
		Set<String> ignoredProperties = new HashSet<>();
		for (int i = 0; i < values.length; i++) {
			String property = "%s[%d]".formatted(key, i);
			assertThat(environment.getProperty(property)).isEqualTo(values[i]);
			ignoredProperties.add(property);
		}
		enumerateAllProperties(environment, ignoredProperties, (name) -> assertThat(name).doesNotStartWith(key + "["));
	}

	private void assertHasEmptyList(Environment environment, String key) {
		assertThat(environment.getProperty(key)).isEqualTo("");
		enumerateAllProperties(environment, (name) -> assertThat(name).doesNotStartWith(key + "["));
	}

	private void assertHasNoList(Environment environment, String key) {
		enumerateAllProperties(environment, (name) -> assertThat(name).doesNotStartWith(key + "["));
	}

	private void enumerateAllProperties(Environment environment, ThrowingConsumer<String> consumer) {
		enumerateAllProperties(environment, Collections.emptySet(), consumer::accept);
	}

	private void enumerateAllProperties(Environment environment, Set<String> ignoredProperties,
			ThrowingConsumer<String> consumer) {
		ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) environment;
		configurableEnvironment.getPropertySources().forEach((source) -> {
			if (source instanceof EnumerablePropertySource<?> enumerablePropertySource) {
				assertThat(enumerablePropertySource.getPropertyNames()).allSatisfy((name) -> {
					if (!ignoredProperties.contains(name)) {
						consumer.accept(name);
					}
				});
			}
			else {
				fail("Property source %s doesn't support enumerating properties", source);
			}
		});
	}

}
