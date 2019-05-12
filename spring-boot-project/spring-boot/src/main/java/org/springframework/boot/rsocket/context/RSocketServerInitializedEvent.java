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
@SuppressWarnings("serial")
public class RSocketServerInitializedEvent extends ApplicationEvent {

	public RSocketServerInitializedEvent(RSocketServer rSocketServer) {
		super(rSocketServer);
	}

	/**
	 * Access the {@link RSocketServer}.
	 * @return the embedded RSocket server
	 */
	public RSocketServer getrSocketServer() {
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
