/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.server;

import io.rsocket.core.RSocketServer;

/**
 * Callback interface that can be used to customize a {@link RSocketServer}.
 *
 * @author Brian Clozel
 * @since 2.3.0
 * @see RSocketServer
 */
@FunctionalInterface
public interface RSocketServerCustomizer {

	/**
	 * Callback to customize a {@link RSocketServer} instance.
	 * @param rSocketServer the RSocket server to customize
	 */
	void customize(RSocketServer rSocketServer);

}
