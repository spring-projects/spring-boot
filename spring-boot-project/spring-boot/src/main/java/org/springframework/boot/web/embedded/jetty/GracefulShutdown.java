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

package org.springframework.boot.web.embedded.jetty;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.core.log.LogMessage;

/**
 * Handles Jetty graceful shutdown.
 *
 * @author Andy Wilkinson
 */
final class GracefulShutdown {

	private static final Log logger = LogFactory.getLog(JettyWebServer.class);

	private final Server server;

	private final Supplier<Integer> activeRequests;

	private volatile boolean shuttingDown = false;

	GracefulShutdown(Server server, Supplier<Integer> activeRequests) {
		this.server = server;
		this.activeRequests = activeRequests;
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		for (Connector connector : this.server.getConnectors()) {
			shutdown(connector);
		}
		this.shuttingDown = true;
		new Thread(() -> awaitShutdown(callback), "jetty-shutdown").start();

	}

	private void shutdown(Connector connector) {
		try {
			connector.shutdown().get();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException ex) {
			// Continue
		}
	}

	private void awaitShutdown(GracefulShutdownCallback callback) {
		while (this.shuttingDown && this.activeRequests.get() > 0) {
			sleep(100);
		}
		this.shuttingDown = false;
		long activeRequests = this.activeRequests.get();
		if (activeRequests == 0) {
			logger.info("Graceful shutdown complete");
			callback.shutdownComplete(GracefulShutdownResult.IDLE);
		}
		else {
			logger.info(LogMessage.format("Graceful shutdown aborted with %d request(s) still active", activeRequests));
			callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
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

	void abort() {
		this.shuttingDown = false;
	}

}
