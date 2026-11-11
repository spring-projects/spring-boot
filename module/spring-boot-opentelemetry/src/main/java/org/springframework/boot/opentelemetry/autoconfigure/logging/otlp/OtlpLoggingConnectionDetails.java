/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure.logging.otlp;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to an OpenTelemetry logging service.
 *
 * @author Toshiaki Maki
 * @since 4.0.0
 */
public interface OtlpLoggingConnectionDetails extends ConnectionDetails {

	/**
	 * Address to where logs will be published.
	 * @param transport the transport to use
	 * @return the address to where logs will be published
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
