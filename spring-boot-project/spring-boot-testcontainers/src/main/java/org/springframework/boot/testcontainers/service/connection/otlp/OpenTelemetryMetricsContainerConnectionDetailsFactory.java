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

package org.springframework.boot.testcontainers.service.connection.otlp;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link OtlpMetricsConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer} using
 * the {@code "otel/opentelemetry-collector-contrib"} image.
 *
 * @author Eddú Meléndez
 */
class OpenTelemetryMetricsContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, OtlpMetricsConnectionDetails> {

	/**
	 * Constructs a new instance of the
	 * {@code OpenTelemetryMetricsContainerConnectionDetailsFactory} class.
	 * @param repositoryName the name of the repository containing the OpenTelemetry
	 * collector-contrib library
	 * @param configurationClassName the fully qualified class name of the
	 * OtlpMetricsExportAutoConfiguration class
	 */
	OpenTelemetryMetricsContainerConnectionDetailsFactory() {
		super("otel/opentelemetry-collector-contrib",
				"org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration");
	}

	/**
	 * Returns the connection details for the given container connection source.
	 * @param source the container connection source
	 * @return the connection details for the container
	 */
	@Override
	protected OtlpMetricsConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<Container<?>> source) {
		return new OpenTelemetryMetricsContainerConnectionDetails(source);
	}

	/**
	 * OpenTelemetryMetricsContainerConnectionDetails class.
	 */
	private static final class OpenTelemetryMetricsContainerConnectionDetails
			extends ContainerConnectionDetails<Container<?>> implements OtlpMetricsConnectionDetails {

		/**
		 * Constructs a new OpenTelemetryMetricsContainerConnectionDetails with the
		 * specified ContainerConnectionSource.
		 * @param source the source of the container connection
		 */
		private OpenTelemetryMetricsContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		/**
		 * Returns the URL for accessing the metrics endpoint. The URL is constructed
		 * using the host and mapped port of the container.
		 * @return the URL for accessing the metrics endpoint
		 */
		@Override
		public String getUrl() {
			return "http://%s:%d/v1/metrics".formatted(getContainer().getHost(), getContainer().getMappedPort(4318));
		}

	}

}
