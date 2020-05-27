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
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.netty.DisposableServer;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;

/**
 * Handles Netty graceful shutdown.
 *
 * @author Andy Wilkinson
 */
final class GracefulShutdown {

	private static final Log logger = LogFactory.getLog(GracefulShutdown.class);

	private final Supplier<DisposableServer> disposableServer;

	private volatile Thread shutdownThread;

	private volatile boolean shuttingDown;

	GracefulShutdown(Supplier<DisposableServer> disposableServer) {
		this.disposableServer = disposableServer;
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		DisposableServer server = this.disposableServer.get();
		if (server == null) {
			return;
		}
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		this.shutdownThread = new Thread(() -> doShutdown(callback, server), "netty-shutdown");
		this.shutdownThread.start();
	}

	private void doShutdown(GracefulShutdownCallback callback, DisposableServer server) {
		this.shuttingDown = true;
		try {
			server.disposeNow(Duration.ofMillis(Long.MAX_VALUE));
			logger.info("Graceful shutdown complete");
			callback.shutdownComplete(GracefulShutdownResult.IDLE);
		}
		catch (Exception ex) {
			logger.info("Graceful shutdown aborted with one or more requests still active");
			callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
		}
		finally {
			this.shutdownThread = null;
			this.shuttingDown = false;
		}
	}

	void abort() {
		Thread shutdownThread = this.shutdownThread;
		if (shutdownThread != null) {
			while (!this.shuttingDown) {
				sleep(50);
			}
			this.shutdownThread.interrupt();
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

}
