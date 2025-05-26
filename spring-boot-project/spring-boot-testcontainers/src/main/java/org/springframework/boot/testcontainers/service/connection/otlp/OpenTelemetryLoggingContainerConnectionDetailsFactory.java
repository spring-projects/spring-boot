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

package org.springframework.boot.testcontainers.service.connection.otlp;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.actuate.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link OtlpLoggingConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer} using
 * the {@code "otel/opentelemetry-collector-contrib"} image.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class OpenTelemetryLoggingContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, OtlpLoggingConnectionDetails> {

	private static final int OTLP_GRPC_PORT = 4317;

	private static final int OTLP_HTTP_PORT = 4318;

	OpenTelemetryLoggingContainerConnectionDetailsFactory() {
		super("otel/opentelemetry-collector-contrib",
				"org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration");
	}

	@Override
	protected OtlpLoggingConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<Container<?>> source) {
		return new OpenTelemetryLoggingContainerConnectionDetails(source);
	}

	private static final class OpenTelemetryLoggingContainerConnectionDetails
			extends ContainerConnectionDetails<Container<?>> implements OtlpLoggingConnectionDetails {

		private OpenTelemetryLoggingContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public String getUrl(Transport transport) {
			int port = switch (transport) {
				case HTTP -> OTLP_HTTP_PORT;
				case GRPC -> OTLP_GRPC_PORT;
			};
			return "http://%s:%d/v1/logs".formatted(getContainer().getHost(), getContainer().getMappedPort(port));
		}

	}

}
