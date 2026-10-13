/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;

/**
 * Factory interface that can be used to create a reactive {@link WebServer}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @see WebServer
 */
@FunctionalInterface
public interface ReactiveWebServerFactory extends WebServerFactory {

	/**
	 * Gets a new fully configured but paused {@link WebServer} instance. Clients should
	 * not be able to connect to the returned server until {@link WebServer#start()} is
	 * called (which happens when the {@code ApplicationContext} has been fully
	 * refreshed).
	 * @param httpHandler the HTTP handler in charge of processing requests
	 * @return a fully configured and started {@link WebServer}
	 * @see WebServer#stop()
	 */
	WebServer getWebServer(HttpHandler httpHandler);

}
