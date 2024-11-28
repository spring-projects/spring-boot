/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.otlp;

import org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.actuate.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpLoggingConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 */
class OpenTelemetryLoggingDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OtlpLoggingConnectionDetails> {

	private static final String[] OPENTELEMETRY_IMAGE_NAMES = { "otel/opentelemetry-collector-contrib",
			"grafana/otel-lgtm" };

	private static final int OTLP_GRPC_PORT = 4317;

	private static final int OTLP_HTTP_PORT = 4318;

	OpenTelemetryLoggingDockerComposeConnectionDetailsFactory() {
		super(OPENTELEMETRY_IMAGE_NAMES,
				"org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration");
	}

	@Override
	protected OtlpLoggingConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OpenTelemetryLoggingDockerComposeConnectionDetails(source.getRunningService());
	}

	private static final class OpenTelemetryLoggingDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OtlpLoggingConnectionDetails {

		private final String host;

		private final int grpcPort;

		private final int httpPort;

		private OpenTelemetryLoggingDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.grpcPort = source.ports().get(OTLP_GRPC_PORT);
			this.httpPort = source.ports().get(OTLP_HTTP_PORT);
		}

		@Override
		public String getUrl(Transport transport) {
			int port = switch (transport) {
				case HTTP -> this.httpPort;
				case GRPC -> this.grpcPort;
			};
			return "http://%s:%d/v1/logs".formatted(this.host, port);
		}

	}

}
