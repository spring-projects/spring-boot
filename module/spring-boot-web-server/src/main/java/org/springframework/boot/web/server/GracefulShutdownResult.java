/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

/**
 * The result of a graceful shutdown request.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 * @see GracefulShutdownCallback
 * @see WebServer#shutDownGracefully(GracefulShutdownCallback)
 */
public enum GracefulShutdownResult {

	/**
	 * Requests remained active at the end of the grace period.
	 */
	REQUESTS_ACTIVE,

	/**
	 * The server was idle with no active requests at the end of the grace period.
	 */
	IDLE,

	/**
	 * The server was shutdown immediately, ignoring any active requests.
	 */
	IMMEDIATE

}
