/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to an OpenTelemetry Collector service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public interface OtlpMetricsConnectionDetails extends ConnectionDetails {

	/**
	 * Address to where metrics will be published.
	 * @return the address to where metrics will be published
	 */
	@Nullable String getUrl();

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use or {@code null}
	 * @since 4.1.0
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

}
