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

import org.testcontainers.grafana.LgtmStackContainer;

import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingConnectionDetails;
import org.springframework.boot.actuate.autoconfigure.tracing.otlp.Transport;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link OtlpTracingConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link LgtmStackContainer} using
 * the {@code "grafana/otel-lgtm"} image.
 *
 * @author Eddú Meléndez
 */
class GrafanaOpenTelemetryTracingContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpTracingConnectionDetails> {

	GrafanaOpenTelemetryTracingContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME,
				"org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration");
	}

	@Override
	protected OtlpTracingConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<LgtmStackContainer> source) {
		return new OpenTelemetryTracingContainerConnectionDetails(source);
	}

	private static final class OpenTelemetryTracingContainerConnectionDetails
			extends ContainerConnectionDetails<LgtmStackContainer> implements OtlpTracingConnectionDetails {

		private OpenTelemetryTracingContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
			super(source);
		}

		@Override
		public String getUrl(Transport transport) {
			String url = switch (transport) {
				case HTTP -> getContainer().getOtlpHttpUrl();
				case GRPC -> getContainer().getOtlpGrpcUrl();
			};
			return "%s/v1/traces".formatted(url);
		}

	}

}
