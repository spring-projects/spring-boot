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

import io.rsocket.SocketAcceptor;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * Bootstrap an {@link RSocketServer} and start it with the application context.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class RSocketServerBootstrap implements ApplicationEventPublisherAware, SmartLifecycle {

	private final RSocketServer server;

	private ApplicationEventPublisher eventPublisher;

	public RSocketServerBootstrap(RSocketServerFactory serverFactory, SocketAcceptor socketAcceptor) {
		Assert.notNull(serverFactory, "ServerFactory must not be null");
		this.server = serverFactory.create(socketAcceptor);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	@Override
	public void start() {
		this.server.start();
		this.eventPublisher.publishEvent(new RSocketServerInitializedEvent(this.server));
	}

	@Override
	public void stop() {
		this.server.stop();
	}

	@Override
	public boolean isRunning() {
		RSocketServer server = this.server;
		if (server != null) {
			return server.address() != null;
		}
		return false;
	}

}
