/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.reactor.netty;

import java.time.Duration;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.netty.DisposableServer;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.util.Assert;

/**
 * Handles Netty graceful shutdown.
 *
 * @author Andy Wilkinson
 */
final class GracefulShutdown {

	private static final Log logger = LogFactory.getLog(GracefulShutdown.class);

	private final Supplier<@Nullable DisposableServer> disposableServer;

	private volatile @Nullable ShutdownState state;

	GracefulShutdown(Supplier<@Nullable DisposableServer> disposableServer) {
		this.disposableServer = disposableServer;
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		DisposableServer server = this.disposableServer.get();
		if (server == null) {
			return;
		}
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		Thread shutdownThread = new Thread(() -> doShutdown(callback, server), "netty-shutdown");
		this.state = new ShutdownState(shutdownThread);
		shutdownThread.start();
	}

	private void doShutdown(GracefulShutdownCallback callback, DisposableServer server) {
		ShutdownState state = this.state;
		Assert.notNull(state, "No Shutdown state available");
		state.shuttingDown = true;
		try {
			server.disposeNow(Duration.ofNanos(Long.MAX_VALUE));
			logger.info("Graceful shutdown complete");
			shutdownComplete(callback, GracefulShutdownResult.IDLE);
		}
		catch (Exception ex) {
			logger.info("Graceful shutdown aborted with one or more requests still active");
			shutdownComplete(callback, GracefulShutdownResult.REQUESTS_ACTIVE);
		}
	}

	private void shutdownComplete(GracefulShutdownCallback callback, GracefulShutdownResult result) {
		this.state = null;
		callback.shutdownComplete(result);
	}

	void abort() {
		ShutdownState state = this.state;
		if (state != null) {
			while (!state.shuttingDown) {
				sleep(50);
			}
			state.thread.interrupt();
		}
	}

	private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private final class ShutdownState {

		private final Thread thread;

		private volatile boolean shuttingDown;

		private ShutdownState(Thread thread) {
			this.thread = thread;
		}

	}

}
