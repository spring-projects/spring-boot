/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.context;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.context.ApplicationEvent;

/**
 * Event to be published after the application context is refreshed and the
 * {@link RSocketServer} is ready. Useful for obtaining the local port of a running
 * server.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class RSocketServerInitializedEvent extends ApplicationEvent {

	public RSocketServerInitializedEvent(RSocketServer server) {
		super(server);
	}

	/**
	 * Access the {@link RSocketServer}.
	 * @return the embedded RSocket server
	 */
	public RSocketServer getServer() {
		return getSource();
	}

	/**
	 * Access the source of the event (an {@link RSocketServer}).
	 * @return the embedded web server
	 */
	@Override
	public RSocketServer getSource() {
		return (RSocketServer) super.getSource();
	}

}
