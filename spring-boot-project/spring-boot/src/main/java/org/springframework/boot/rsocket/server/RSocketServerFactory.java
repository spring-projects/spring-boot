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
