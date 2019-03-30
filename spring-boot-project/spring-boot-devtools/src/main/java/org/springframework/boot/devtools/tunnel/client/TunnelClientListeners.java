/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.util.Assert;

/**
 * A collection of {@link TunnelClientListener}.
 *
 * @author Phillip Webb
 */
class TunnelClientListeners {

	private final List<TunnelClientListener> listeners = new CopyOnWriteArrayList<>();

	public void addListener(TunnelClientListener listener) {
		Assert.notNull(listener, "Listener must not be null");
		this.listeners.add(listener);
	}

	public void removeListener(TunnelClientListener listener) {
		Assert.notNull(listener, "Listener must not be null");
		this.listeners.remove(listener);
	}

	public void fireOpenEvent(SocketChannel socket) {
		for (TunnelClientListener listener : this.listeners) {
			listener.onOpen(socket);
		}
	}

	public void fireCloseEvent(SocketChannel socket) {
		for (TunnelClientListener listener : this.listeners) {
			listener.onClose(socket);
		}
	}

}
