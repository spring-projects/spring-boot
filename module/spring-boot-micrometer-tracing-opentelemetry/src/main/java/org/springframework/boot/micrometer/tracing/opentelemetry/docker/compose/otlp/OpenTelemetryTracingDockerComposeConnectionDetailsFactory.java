/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.docker.compose.otlp;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.Transport;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpTracingConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author WooSeung Kim
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

		private final ConnectionPorts ports;

		private OpenTelemetryTracingDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.ports = source.ports();
		}

		@Override
		public String getUrl(Transport transport) {
			int port = switch (transport) {
				case HTTP -> OTLP_HTTP_PORT;
				case GRPC -> OTLP_GRPC_PORT;
			};
			return "http://%s:%d/v1/traces".formatted(this.host, this.ports.get(port));
		}

	}

}
