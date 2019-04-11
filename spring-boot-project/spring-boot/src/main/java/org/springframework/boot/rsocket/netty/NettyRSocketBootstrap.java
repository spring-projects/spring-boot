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

package org.springframework.boot.rsocket.netty;

import org.springframework.boot.rsocket.context.RSocketServerInitializedEvent;
import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.rsocket.MessageHandlerAcceptor;

/**
 * Bootstrap an {@link RSocketServer} and start it with the application context.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class NettyRSocketBootstrap
		implements ApplicationEventPublisherAware, SmartLifecycle {

	private final RSocketServer rSocketServer;

	private ApplicationEventPublisher applicationEventPublisher;

	public NettyRSocketBootstrap(RSocketServerFactory serverFactoryProvider,
			MessageHandlerAcceptor messageHandlerAcceptorProvider) {
		this.rSocketServer = serverFactoryProvider.create(messageHandlerAcceptorProvider);
	}

	@Override
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void start() {
		this.rSocketServer.start();
		this.applicationEventPublisher
				.publishEvent(new RSocketServerInitializedEvent(this.rSocketServer));
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
