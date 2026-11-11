/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp;

/**
 * Transport used to send OTLP data.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public enum Transport {

	/**
	 * HTTP transport.
	 */
	HTTP,

	/**
	 * gRPC transport.
	 */
	GRPC

}
