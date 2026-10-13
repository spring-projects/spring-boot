/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive;

import java.util.Map;

import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;

/**
 * A mock reactive {@link WebServer}.
 *
 * @author Brian Clozel
 */
public class MockReactiveWebServer implements WebServer {

	private final int port;

	private HttpHandler httpHandler;

	private Map<String, HttpHandler> httpHandlerMap;

	MockReactiveWebServer(HttpHandler httpHandler, int port) {
		this.httpHandler = httpHandler;
		this.port = port;
	}

	MockReactiveWebServer(Map<String, HttpHandler> httpHandlerMap, int port) {
		this.httpHandlerMap = httpHandlerMap;
		this.port = port;
	}

	public HttpHandler getHttpHandler() {
		return this.httpHandler;
	}

	public Map<String, HttpHandler> getHttpHandlerMap() {
		return this.httpHandlerMap;
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

	}

	@Override
	public int getPort() {
		return this.port;
	}

}
