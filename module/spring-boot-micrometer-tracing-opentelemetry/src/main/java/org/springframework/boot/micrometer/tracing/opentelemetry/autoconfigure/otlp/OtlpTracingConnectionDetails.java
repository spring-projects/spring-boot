/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

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

}
