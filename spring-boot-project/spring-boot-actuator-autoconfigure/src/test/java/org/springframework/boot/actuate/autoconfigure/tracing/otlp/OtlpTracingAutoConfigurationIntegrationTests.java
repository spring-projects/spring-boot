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

package org.springframework.boot.actuate.autoconfigure.tracing.otlp;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.micrometer.tracing.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.GzipSource;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfigurationIntegrationTests.MockGrpcServer.RecordedGrpcRequest;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtlpTracingAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class OtlpTracingAutoConfigurationIntegrationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("management.tracing.sampling.probability=1.0")
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class,
				MicrometerTracingAutoConfiguration.class, OpenTelemetryAutoConfiguration.class,
				OpenTelemetryTracingAutoConfiguration.class, OtlpTracingAutoConfiguration.class));

	private final MockWebServer mockWebServer = new MockWebServer();

	private final MockGrpcServer mockGrpcServer = new MockGrpcServer();

	@BeforeEach
	void startServers() throws Exception {
		this.mockWebServer.start();
		this.mockGrpcServer.start();
	}

	@AfterEach
	void stopServers() throws Exception {
		this.mockWebServer.close();
		this.mockGrpcServer.close();
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

	@Test
	void grpcSpanExporterShouldExportSpans() {
		this.contextRunner
			.withPropertyValues(
					"management.otlp.tracing.endpoint=http://localhost:%d".formatted(this.mockGrpcServer.getPort()),
					"management.otlp.tracing.headers.custom=42", "management.otlp.tracing.transport=grpc")
			.run((context) -> {
				context.getBean(Tracer.class).nextSpan().name("test").end();
				assertThat(context.getBean(OtlpGrpcSpanExporter.class).flush())
					.isSameAs(CompletableResultCode.ofSuccess());
				RecordedGrpcRequest request = this.mockGrpcServer.takeRequest(10, TimeUnit.SECONDS);
				assertThat(request).isNotNull();
				assertThat(request.headers().get("Content-Type")).isEqualTo("application/grpc");
				assertThat(request.headers().get("custom")).isEqualTo("42");
				assertThat(request.bodyAsString()).contains("org.springframework.boot");
			});
	}

	static class MockGrpcServer {

		private final Server server = createServer();

		private final BlockingQueue<RecordedGrpcRequest> recordedRequests = new LinkedBlockingQueue<>();

		void start() throws Exception {
			this.server.start();
		}

		void close() throws Exception {
			this.server.stop();
		}

		int getPort() {
			return this.server.getURI().getPort();
		}

		RecordedGrpcRequest takeRequest(int timeout, TimeUnit unit) throws InterruptedException {
			return this.recordedRequests.poll(timeout, unit);
		}

		void recordRequest(RecordedGrpcRequest request) {
			this.recordedRequests.add(request);
		}

		private Server createServer() {
			Server server = new Server();
			server.addConnector(createConnector(server));
			server.setHandler(new GrpcHandler());
			return server;
		}

		private ServerConnector createConnector(Server server) {
			ServerConnector connector = new ServerConnector(server,
					new HTTP2CServerConnectionFactory(new HttpConfiguration()));
			connector.setPort(0);
			return connector;
		}

		class GrpcHandler extends Handler.Abstract {

			@Override
			public boolean handle(Request request, Response response, Callback callback) throws Exception {
				try (InputStream in = Content.Source.asInputStream(request)) {
					recordRequest(new RecordedGrpcRequest(request.getHeaders(), in.readAllBytes()));
				}
				response.getHeaders().add("Content-Type", "application/grpc");
				response.getHeaders().add("Grpc-Status", "0");
				callback.succeeded();
				return true;
			}

		}

		record RecordedGrpcRequest(HttpFields headers, byte[] body) {
			String bodyAsString() {
				return new String(this.body, StandardCharsets.UTF_8);
			}
		}

	}

}
