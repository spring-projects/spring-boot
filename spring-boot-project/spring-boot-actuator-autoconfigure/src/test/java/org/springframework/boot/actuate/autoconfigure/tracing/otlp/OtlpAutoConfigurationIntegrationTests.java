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

package org.springframework.boot.actuate.autoconfigure.tracing.otlp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import io.micrometer.tracing.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.GzipSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtlpAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class OtlpAutoConfigurationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("management.tracing.sampling.probability=1.0")
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
				MicrometerTracingAutoConfiguration.class, OpenTelemetryAutoConfiguration.class,
				org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration.class,
				OtlpAutoConfiguration.class));

	private final MockWebServer mockWebServer = new MockWebServer();

	@BeforeEach
	void setUp() throws IOException {
		this.mockWebServer.start();
	}

	@AfterEach
	void tearDown() throws IOException {
		this.mockWebServer.close();
	}

	@Test
	void httpSpanExporterShouldUseProtobufAndNoCompressionByDefault() {
		this.mockWebServer.enqueue(new MockResponse());
		this.contextRunner
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:%d/v1/traces"
				.formatted(this.mockWebServer.getPort()), "management.otlp.tracing.headers.custom=42")
			.run((context) -> {
				context.getBean(Tracer.class).nextSpan().name("test").end();
				assertThat(context.getBean(OtlpHttpSpanExporter.class).flush())
					.isSameAs(CompletableResultCode.ofSuccess());
				RecordedRequest request = this.mockWebServer.takeRequest(10, TimeUnit.SECONDS);
				assertThat(request).isNotNull();
				assertThat(request.getRequestLine()).contains("/v1/traces");
				assertThat(request.getHeader("Content-Type")).isEqualTo("application/x-protobuf");
				assertThat(request.getHeader("custom")).isEqualTo("42");
				assertThat(request.getBodySize()).isPositive();
				try (Buffer body = request.getBody()) {
					assertThat(body.readString(StandardCharsets.UTF_8)).contains("org.springframework.boot");
				}
			});
	}

	@Test
	void httpSpanExporterCanBeConfiguredToUseGzipCompression() {
		this.mockWebServer.enqueue(new MockResponse());
		this.contextRunner
			.withPropertyValues("management.otlp.tracing.compression=gzip",
					"management.otlp.tracing.endpoint=http://localhost:%d/test".formatted(this.mockWebServer.getPort()))
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class).hasSingleBean(SpanExporter.class);
				context.getBean(Tracer.class).nextSpan().name("test").end();
				assertThat(context.getBean(OtlpHttpSpanExporter.class).flush())
					.isSameAs(CompletableResultCode.ofSuccess());
				RecordedRequest request = this.mockWebServer.takeRequest(10, TimeUnit.SECONDS);
				assertThat(request).isNotNull();
				assertThat(request.getRequestLine()).contains("/test");
				assertThat(request.getHeader("Content-Type")).isEqualTo("application/x-protobuf");
				assertThat(request.getHeader("Content-Encoding")).isEqualTo("gzip");
				assertThat(request.getBodySize()).isPositive();
				try (Buffer uncompressed = new Buffer(); Buffer body = request.getBody()) {
					uncompressed.writeAll(new GzipSource(body));
					assertThat(uncompressed.readString(StandardCharsets.UTF_8)).contains("org.springframework.boot");
				}
			});
	}

}
