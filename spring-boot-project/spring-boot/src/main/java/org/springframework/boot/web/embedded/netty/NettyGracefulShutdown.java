/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.embedded.netty;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import org.springframework.boot.web.server.GracefulShutdown;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

/**
 * {@link GracefulShutdown} for a Reactor Netty {@link DisposableServer}.
 *
 * @author Andy Wilkinson
 */
final class NettyGracefulShutdown implements GracefulShutdown {

	private static final Log logger = LogFactory.getLog(NettyGracefulShutdown.class);

	private final Supplier<DisposableServer> disposableServer;

	private final Duration lifecycleTimeout;

	private final Duration period;

	private final AtomicLong activeRequests = new AtomicLong();

	private volatile boolean shuttingDown;

	NettyGracefulShutdown(Supplier<DisposableServer> disposableServer, Duration lifecycleTimeout, Duration period) {
		this.disposableServer = disposableServer;
		this.lifecycleTimeout = lifecycleTimeout;
		this.period = period;
	}

	@Override
	public boolean shutDownGracefully() {
		logger.info("Commencing graceful shutdown, allowing up to " + this.period.getSeconds()
				+ "s for active requests to complete");
		DisposableServer server = this.disposableServer.get();
		if (server == null) {
			return false;
		}
		if (this.lifecycleTimeout != null) {
			server.disposeNow(this.lifecycleTimeout);
		}
		else {
			server.disposeNow();
		}
		this.shuttingDown = true;
		long end = System.currentTimeMillis() + this.period.toMillis();
		try {
			while (this.activeRequests.get() > 0 && System.currentTimeMillis() < end) {
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			long activeRequests = this.activeRequests.get();
			if (activeRequests == 0) {
				logger.info("Graceful shutdown complete");
				return true;
			}
			if (logger.isInfoEnabled()) {
				logger.info("Grace period elaped with " + activeRequests + " request(s) still active");
			}
			return false;
		}
		finally {
			this.shuttingDown = false;
		}
	}

	@Override
	public boolean isShuttingDown() {
		return this.shuttingDown;
	}

	BiFunction<? super HttpServerRequest, ? super HttpServerResponse, ? extends Publisher<Void>> wrapHandler(
			ReactorHttpHandlerAdapter handlerAdapter) {
		if (this.period == null) {
			return handlerAdapter;
		}
		return (request, response) -> {
			this.activeRequests.incrementAndGet();
			return handlerAdapter.apply(request, response).doOnTerminate(() -> this.activeRequests.decrementAndGet());
		};
	}

}
