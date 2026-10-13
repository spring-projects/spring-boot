/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive;

import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;

import static org.mockito.Mockito.spy;

/**
 * Mock {@link ReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
public class MockReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private MockReactiveWebServer webServer;

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		this.webServer = spy(new MockReactiveWebServer(httpHandler, getPort()));
		return this.webServer;
	}

	public MockReactiveWebServer getWebServer() {
		return this.webServer;
	}

}
