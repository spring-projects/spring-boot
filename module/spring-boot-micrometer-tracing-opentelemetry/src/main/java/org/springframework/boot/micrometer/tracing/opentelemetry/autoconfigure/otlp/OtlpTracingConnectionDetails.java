/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to an OpenTelemetry service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public interface OtlpTracingConnectionDetails extends ConnectionDetails {

	/**
	 * Address to where tracing will be published.
	 * @param transport the transport to use
	 * @return the address to where tracing will be published
	 */
	String getUrl(Transport transport);

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use or {@code null}
	 * @since 4.1.0
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

}
