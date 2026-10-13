/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

/**
 * Simple interface that represents a fully configured web server (for example Tomcat,
 * Jetty, Netty). Allows the server to be {@link #start() started} and {@link #stop()
 * stopped}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @since 2.0.0
 */
public interface WebServer {

	/**
	 * Starts the web server. Calling this method on an already started server has no
	 * effect.
	 * @throws WebServerException if the server cannot be started
	 */
	void start() throws WebServerException;

	/**
	 * Stops the web server. Calling this method on an already stopped server has no
	 * effect.
	 * @throws WebServerException if the server cannot be stopped
	 */
	void stop() throws WebServerException;

	/**
	 * Return the port this server is listening on.
	 * @return the port (or -1 if none)
	 */
	int getPort();

	/**
	 * Initiates a graceful shutdown of the web server. Handling of new requests is
	 * prevented and the given {@code callback} is invoked at the end of the attempt. The
	 * attempt can be explicitly ended by invoking {@link #stop}. The default
	 * implementation invokes the callback immediately with
	 * {@link GracefulShutdownResult#IMMEDIATE}, i.e. no attempt is made at a graceful
	 * shutdown.
	 * @param callback the callback to invoke when the graceful shutdown completes
	 * @since 2.3.0
	 */
	default void shutDownGracefully(GracefulShutdownCallback callback) {
		callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
	}

	/**
	 * Destroys the web server such that it cannot be started again.
	 * @since 3.2.0
	 */
	default void destroy() {
		stop();
	}

}
