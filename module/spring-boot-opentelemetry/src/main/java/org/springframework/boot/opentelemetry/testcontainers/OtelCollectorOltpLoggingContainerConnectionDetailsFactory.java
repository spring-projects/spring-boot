/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.boot.ssl.SslBundle;
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
class OtelCollectorOltpLoggingContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, OtlpLoggingConnectionDetails> {

	private static final int OTLP_GRPC_PORT = 4317;

	private static final int OTLP_HTTP_PORT = 4318;

	OtelCollectorOltpLoggingContainerConnectionDetailsFactory() {
		super("otel/opentelemetry-collector-contrib");
	}

	@Override
	protected OtlpLoggingConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<Container<?>> source) {
		return new OtlpLoggingContainerConnectionDetails(source);
	}

	private static final class OtlpLoggingContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements OtlpLoggingConnectionDetails {

		private OtlpLoggingContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public String getUrl(Transport transport) {
			int port = switch (transport) {
				case HTTP -> OTLP_HTTP_PORT;
				case GRPC -> OTLP_GRPC_PORT;
			};
			String scheme = (getSslBundle() != null) ? "https" : "http";
			return "%s://%s:%d/v1/logs".formatted(scheme, getContainer().getHost(), getContainer().getMappedPort(port));
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
