/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.docker.compose.otlp;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.Transport;
import org.springframework.boot.ssl.SslBundle;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpTracingConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class OpenTelemetryTracingDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OtlpTracingConnectionDetails> {

	private static final String[] OPENTELEMETRY_IMAGE_NAMES = { "otel/opentelemetry-collector-contrib",
			"grafana/otel-lgtm" };

	private static final int OTLP_GRPC_PORT = 4317;

	private static final int OTLP_HTTP_PORT = 4318;

	OpenTelemetryTracingDockerComposeConnectionDetailsFactory() {
		super(OPENTELEMETRY_IMAGE_NAMES,
				"org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration");
	}

	@Override
	protected OtlpTracingConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OpenTelemetryTracingDockerComposeConnectionDetails(source.getRunningService());
	}

	private static final class OpenTelemetryTracingDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OtlpTracingConnectionDetails {

		private final String host;

		private final int grpcPort;

		private final int httpPort;

		private final @Nullable SslBundle sslBundle;

		private OpenTelemetryTracingDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.grpcPort = source.ports().get(OTLP_GRPC_PORT);
			this.httpPort = source.ports().get(OTLP_HTTP_PORT);
			this.sslBundle = getSslBundle(source);
		}

		@Override
		public String getUrl(Transport transport) {
			int port = switch (transport) {
				case HTTP -> this.httpPort;
				case GRPC -> this.grpcPort;
			};
			String scheme = (this.sslBundle != null) ? "https" : "http";
			return "%s://%s:%d/v1/traces".formatted(scheme, this.host, port);
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

	}

}
