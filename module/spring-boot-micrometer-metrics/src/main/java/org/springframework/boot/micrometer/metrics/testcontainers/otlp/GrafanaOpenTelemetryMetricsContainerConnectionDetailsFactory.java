/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.testcontainers.otlp;

import org.jspecify.annotations.Nullable;
import org.testcontainers.grafana.LgtmStackContainer;

import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
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
				"org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration");
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
			String url = getContainer().getOtlpHttpUrl();
			if (getSslBundle() != null) {
				url = url.replaceFirst("^http://", "https://");
			}
			return "%s/v1/metrics".formatted(url);
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
