/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.ssl.SslBundle;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpLoggingConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 */
class OtlpLoggingDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OtlpLoggingConnectionDetails> {

	private static final String[] OPENTELEMETRY_IMAGE_NAMES = { "otel/opentelemetry-collector-contrib",
			"grafana/otel-lgtm" };

	private static final int OTLP_GRPC_PORT = 4317;

	private static final int OTLP_HTTP_PORT = 4318;

	OtlpLoggingDockerComposeConnectionDetailsFactory() {
		super(OPENTELEMETRY_IMAGE_NAMES,
				"org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration");
	}

	@Override
	protected OtlpLoggingConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OtlpLoggingDockerComposeConnectionDetails(source.getRunningService());
	}

	private static final class OtlpLoggingDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OtlpLoggingConnectionDetails {

		private final String host;

		private final int grpcPort;

		private final int httpPort;

		private final @Nullable SslBundle sslBundle;

		private OtlpLoggingDockerComposeConnectionDetails(RunningService source) {
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
			return "%s://%s:%d/v1/logs".formatted(scheme, this.host, port);
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

	}

}
