/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;

/**
 * Event to be published after the {@link WebServer} is ready. Useful for obtaining the
 * local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class ReactiveWebServerInitializedEvent extends WebServerInitializedEvent {

	private final ReactiveWebServerApplicationContext applicationContext;

	public ReactiveWebServerInitializedEvent(WebServer webServer,
			ReactiveWebServerApplicationContext applicationContext) {
		super(webServer);
		this.applicationContext = applicationContext;
	}

	@Override
	public ReactiveWebServerApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
