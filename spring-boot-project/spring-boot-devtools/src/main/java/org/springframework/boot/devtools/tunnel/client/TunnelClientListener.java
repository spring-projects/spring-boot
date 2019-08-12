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

package org.springframework.boot.devtools.tunnel.client;

import java.nio.channels.SocketChannel;

/**
 * Listener that can be used to receive {@link TunnelClient} events.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public interface TunnelClientListener {

	/**
	 * Called when a socket channel is opened.
	 * @param socket the socket channel
	 */
	void onOpen(SocketChannel socket);

	/**
	 * Called when a socket channel is closed.
	 * @param socket the socket channel
	 */
	void onClose(SocketChannel socket);

}
