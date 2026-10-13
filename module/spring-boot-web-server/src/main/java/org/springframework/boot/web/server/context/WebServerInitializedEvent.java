/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationEvent;

/**
 * Event to be published when the {@link WebServer} is ready. Useful for obtaining the
 * local port of a running server.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@SuppressWarnings("serial")
public abstract class WebServerInitializedEvent extends ApplicationEvent {

	protected WebServerInitializedEvent(WebServer webServer) {
		super(webServer);
	}

	/**
	 * Access the {@link WebServer}.
	 * @return the embedded web server
	 */
	public WebServer getWebServer() {
		return getSource();
	}

	/**
	 * Access the application context that the server was created in. Sometimes it is
	 * prudent to check that this matches expectations (like being equal to the current
	 * context) before acting on the server itself.
	 * @return the applicationContext that the server was created from
	 */
	public abstract WebServerApplicationContext getApplicationContext();

	/**
	 * Access the source of the event (an {@link WebServer}).
	 * @return the embedded web server
	 */
	@Override
	public WebServer getSource() {
		return (WebServer) super.getSource();
	}

}
