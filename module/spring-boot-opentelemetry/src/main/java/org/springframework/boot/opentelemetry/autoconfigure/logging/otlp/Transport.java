/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.opentelemetry.autoconfigure.logging.otlp;

/**
 * Transport used to send OTLP log data.
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
