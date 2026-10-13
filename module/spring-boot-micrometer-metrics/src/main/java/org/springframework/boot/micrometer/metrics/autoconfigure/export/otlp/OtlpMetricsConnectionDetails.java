/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to an OpenTelemetry Collector service.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
public interface OtlpMetricsConnectionDetails extends ConnectionDetails {

	/**
	 * Address to where metrics will be published.
	 * @return the address to where metrics will be published
	 */
	@Nullable String getUrl();

}
