/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.rsocket.server;

import java.net.InetSocketAddress;

/**
 * Simple interface that represents a fully configured RSocket server. Allows the server
 * to be {@link #start() started} and {@link #stop() stopped}.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public interface RSocketServer {

	/**
	 * Starts the RSocket server. Calling this method on an already started server has no
	 * effect.
	 * @throws RSocketServerException if the server cannot be started
	 */
	void start() throws RSocketServerException;

	/**
	 * Stops the RSocket server. Calling this method on an already stopped server has no
	 * effect.
	 * @throws RSocketServerException if the server cannot be stopped
	 */
	void stop() throws RSocketServerException;

	/**
	 * Return the address this server is listening on.
	 * @return the address
	 */
	InetSocketAddress address();

	/**
	 * Choice of transport protocol for the RSocket server.
	 */
	enum TRANSPORT {

		TCP, WEBSOCKET

	}

}
