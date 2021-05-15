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

import java.net.InetSocketAddress;
import java.time.Duration;

import io.rsocket.transport.netty.server.CloseableChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.RSocketServerException;
import org.springframework.util.Assert;

/**
 * {@link RSocketServer} that is based on a Reactor Netty server. Usually this class
 * should be created using the {@link NettyRSocketServerFactory} and not directly.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
public class NettyRSocketServer implements RSocketServer {

	private static final Log logger = LogFactory.getLog(NettyRSocketServer.class);

	private final Mono<CloseableChannel> starter;

	private final Duration lifecycleTimeout;

	private CloseableChannel channel;

	public NettyRSocketServer(Mono<CloseableChannel> starter, Duration lifecycleTimeout) {
		Assert.notNull(starter, "starter must not be null");
		this.starter = starter;
		this.lifecycleTimeout = lifecycleTimeout;
	}

	@Override
	public InetSocketAddress address() {
		if (this.channel != null) {
			return this.channel.address();
		}
		return null;
	}

	@Override
	public void start() throws RSocketServerException {
		this.channel = block(this.starter, this.lifecycleTimeout);
		logger.info("Netty RSocket started on port(s): " + address().getPort());
		startDaemonAwaitThread(this.channel);
	}

	private void startDaemonAwaitThread(CloseableChannel channel) {
		Thread awaitThread = new Thread(() -> channel.onClose().block(), "rsocket");
		awaitThread.setContextClassLoader(getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}

	@Override
	public void stop() throws RSocketServerException {
		if (this.channel != null) {
			this.channel.dispose();
			this.channel = null;
		}
	}

	private <T> T block(Mono<T> mono, Duration timeout) {
		return (timeout != null) ? mono.block(timeout) : mono.block();
	}

}
