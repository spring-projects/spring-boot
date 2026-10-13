/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

/**
 * A callback for the result of a graceful shutdown request.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 * @see WebServer#shutDownGracefully(GracefulShutdownCallback)
 */
@FunctionalInterface
public interface GracefulShutdownCallback {

	/**
	 * Graceful shutdown has completed with the given {@code result}.
	 * @param result the result of the shutdown
	 */
	void shutdownComplete(GracefulShutdownResult result);

}
