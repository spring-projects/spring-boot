/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
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
 * @author WooSeung Kim
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

		private final ConnectionPorts ports;

		private final @Nullable SslBundle sslBundle;

		private OtlpLoggingDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.ports = source.ports();
			this.sslBundle = getSslBundle(source);
		}

		@Override
		public String getUrl(Transport transport) {
			int port = switch (transport) {
				case HTTP -> OTLP_HTTP_PORT;
				case GRPC -> OTLP_GRPC_PORT;
			};
			String scheme = (this.sslBundle != null) ? "https" : "http";
			return "%s://%s:%d/v1/logs".formatted(scheme, this.host, this.ports.get(port));
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

	}

}
