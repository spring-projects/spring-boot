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

import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link OtlpMetricsConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link LgtmStackContainer} using
 * the {@code "grafana/otel-lgtm"} image.
 *
 * @author Eddú Meléndez
 */
class GrafanaOpenTelemetryMetricsContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpMetricsConnectionDetails> {

	GrafanaOpenTelemetryMetricsContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME,
				"org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration");
	}

	@Override
	protected OtlpMetricsConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<LgtmStackContainer> source) {
		return new OpenTelemetryMetricsContainerConnectionDetails(source);
	}

	private static final class OpenTelemetryMetricsContainerConnectionDetails
			extends ContainerConnectionDetails<LgtmStackContainer> implements OtlpMetricsConnectionDetails {

		private OpenTelemetryMetricsContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
			super(source);
		}

		@Override
		public String getUrl() {
			return "%s/v1/metrics".formatted(getContainer().getOtlpHttpUrl());
		}

	}

}
