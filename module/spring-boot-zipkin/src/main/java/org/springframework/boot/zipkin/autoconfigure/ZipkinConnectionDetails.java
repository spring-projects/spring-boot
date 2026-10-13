/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.autoconfigure;

import zipkin2.reporter.HttpEndpointSupplier.Factory;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a Zipkin server.
 * <p>
 * Note: {@linkplain #getSpanEndpoint()} is only read once and passed to a bean of type
 * {@link Factory HttpEndpointSupplier.Factory} which defaults to no-op (constant).
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public interface ZipkinConnectionDetails extends ConnectionDetails {

	/**
	 * The endpoint for the span reporting.
	 * @return the endpoint
	 */
	String getSpanEndpoint();

}
