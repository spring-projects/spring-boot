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

import org.springframework.boot.rsocket.context.RSocketServerInitializedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;

/**
 * Bootstrap an {@link RSocketServer} and start it with the application context.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class RSocketServerBootstrap implements ApplicationEventPublisherAware, SmartLifecycle {

	private final RSocketServer rSocketServer;

	private ApplicationEventPublisher applicationEventPublisher;

	public RSocketServerBootstrap(RSocketServerFactory serverFactoryProvider, SocketAcceptor socketAcceptor) {
		this.rSocketServer = serverFactoryProvider.create(socketAcceptor);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void start() {
		this.rSocketServer.start();
		this.applicationEventPublisher.publishEvent(new RSocketServerInitializedEvent(this.rSocketServer));
	}

	@Override
	public void stop() {
		this.rSocketServer.stop();
	}

	@Override
	public boolean isRunning() {
		RSocketServer server = this.rSocketServer;
		if (server != null) {
			return server.address() != null;
		}
		return false;
	}

}
