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

package org.springframework.boot.docker.compose.service.connection.otlp;

import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpMetricsConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 */
class OpenTelemetryMetricsDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OtlpMetricsConnectionDetails> {

	private static final int OTLP_PORT = 4318;

	/**
	 * Constructs a new instance of the
	 * OpenTelemetryMetricsDockerComposeConnectionDetailsFactory class.
	 * @param imageName the name of the Docker image to use for the OpenTelemetry
	 * Collector
	 * @param autoConfigClassName the fully qualified class name of the auto-configuration
	 * class for exporting metrics to OTLP
	 */
	OpenTelemetryMetricsDockerComposeConnectionDetailsFactory() {
		super("otel/opentelemetry-collector-contrib",
				"org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration");
	}

	/**
	 * Returns the connection details for a Docker Compose source.
	 * @param source the Docker Compose connection source
	 * @return the connection details for the Docker Compose source
	 */
	@Override
	protected OtlpMetricsConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OpenTelemetryMetricsDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * OpenTelemetryMetricsDockerComposeConnectionDetails class.
	 */
	private static final class OpenTelemetryMetricsDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OtlpMetricsConnectionDetails {

		private final String host;

		private final int port;

		/**
		 * Constructs a new OpenTelemetryMetricsDockerComposeConnectionDetails object with
		 * the provided RunningService source.
		 * @param source the RunningService object representing the source of the
		 * connection details
		 */
		private OpenTelemetryMetricsDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.port = source.ports().get(OTLP_PORT);
		}

		/**
		 * Returns the URL for the metrics endpoint.
		 * @return the URL for the metrics endpoint
		 */
		@Override
		public String getUrl() {
			return "http://%s:%d/v1/metrics".formatted(this.host, this.port);
		}

	}

}
