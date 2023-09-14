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
class OpenTelemetryMetricsConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, OtlpMetricsConnectionDetails> {

	OpenTelemetryMetricsConnectionDetailsFactory() {
		super("otel/opentelemetry-collector-contrib",
				"org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration");
	}

	@Override
	protected OtlpMetricsConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<Container<?>> source) {
		return new OpenTelemetryContainerMetricsConnectionDetails(source);
	}

	private static final class OpenTelemetryContainerMetricsConnectionDetails
			extends ContainerConnectionDetails<Container<?>> implements OtlpMetricsConnectionDetails {

		private OpenTelemetryContainerMetricsConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public String getUrl() {
			return "http://%s:%d/v1/metrics".formatted(getContainer().getHost(), getContainer().getMappedPort(4318));
		}

	}

}
