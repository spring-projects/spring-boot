/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.server;

import io.rsocket.SocketAcceptor;

/**
 * Factory interface that can be used to create a reactive {@link RSocketServer}.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@FunctionalInterface
public interface RSocketServerFactory {

	/**
	 * Gets a new fully configured but paused {@link RSocketServer} instance. Clients
	 * should not be able to connect to the returned server until
	 * {@link RSocketServer#start()} is called (which happens when the
	 * {@code ApplicationContext} has been fully refreshed).
	 * @param socketAcceptor the socket acceptor
	 * @return a fully configured and started {@link RSocketServer}
	 * @see RSocketServer#stop()
	 */
	RSocketServer create(SocketAcceptor socketAcceptor);

}
