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

import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OtlpTracingConnectionDetails} for an OTLP service.
 *
 * @author Eddú Meléndez
 */
class OpenTelemetryTracingDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OtlpTracingConnectionDetails> {

	private static final int OTLP_PORT = 4318;

	/**
     * Constructs a new instance of the OpenTelemetryTracingDockerComposeConnectionDetailsFactory class.
     * 
     * This class extends the superclass "otel/opentelemetry-collector-contrib" and is used for configuring
     * the OpenTelemetry tracing with Docker Compose connection details. It also utilizes the
     * "org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpAutoConfiguration" class for
     * auto-configuration of the tracing with OTLP (OpenTelemetry Protocol).
     */
    OpenTelemetryTracingDockerComposeConnectionDetailsFactory() {
		super("otel/opentelemetry-collector-contrib",
				"org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpAutoConfiguration");
	}

	/**
     * Returns the connection details for a Docker Compose setup.
     * 
     * @param source the Docker Compose connection source
     * @return the connection details for the Docker Compose setup
     */
    @Override
	protected OtlpTracingConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OpenTelemetryTracingDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
     * OpenTelemetryTracingDockerComposeConnectionDetails class.
     */
    private static final class OpenTelemetryTracingDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OtlpTracingConnectionDetails {

		private final String host;

		private final int port;

		/**
         * Constructs a new OpenTelemetryTracingDockerComposeConnectionDetails object with the provided RunningService source.
         * 
         * @param source the RunningService object representing the source of the connection details
         */
        private OpenTelemetryTracingDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.port = source.ports().get(OTLP_PORT);
		}

		/**
         * Returns the URL for the OpenTelemetry tracing endpoint.
         * 
         * @return the URL for the OpenTelemetry tracing endpoint
         */
        @Override
		public String getUrl() {
			return "http://%s:%d/v1/traces".formatted(this.host, this.port);
		}

	}

}
