/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.grafana.LgtmStackContainer;

import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link OtlpLoggingConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link LgtmStackContainer} using
 * the {@code "grafana/otel-lgtm"} image.
 *
 * @author Eddú Meléndez
 */
class GrafanaOtlpLoggingContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<LgtmStackContainer, OtlpLoggingConnectionDetails> {

	GrafanaOtlpLoggingContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME);
	}

	@Override
	protected OtlpLoggingConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<LgtmStackContainer> source) {
		return new OtlpLoggingContainerConnectionDetails(source);
	}

	private static final class OtlpLoggingContainerConnectionDetails
			extends ContainerConnectionDetails<LgtmStackContainer> implements OtlpLoggingConnectionDetails {

		private OtlpLoggingContainerConnectionDetails(ContainerConnectionSource<LgtmStackContainer> source) {
			super(source);
		}

		@Override
		public String getUrl(Transport transport) {
			String url = switch (transport) {
				case HTTP -> getContainer().getOtlpHttpUrl();
				case GRPC -> getContainer().getOtlpGrpcUrl();
			};
			if (getSslBundle() != null) {
				url = url.replaceFirst("^http://", "https://");
			}
			return "%s/v1/logs".formatted(url);
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
