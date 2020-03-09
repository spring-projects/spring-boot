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

import java.time.Duration;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import org.springframework.boot.web.server.GracefulShutdown;

/**
 * {@link GracefulShutdown} for Jetty.
 *
 * @author Andy Wilkinson
 */
class JettyGracefulShutdown implements GracefulShutdown {

	private static final Log logger = LogFactory.getLog(JettyGracefulShutdown.class);

	private final Server server;

	private final Supplier<Integer> activeRequests;

	private final Duration period;

	private volatile boolean shuttingDown = false;

	JettyGracefulShutdown(Server server, Supplier<Integer> activeRequests, Duration period) {
		this.server = server;
		this.activeRequests = activeRequests;
		this.period = period;
	}

	@Override
	public boolean shutDownGracefully() {
		logger.info("Commencing graceful shutdown, allowing up to " + this.period.getSeconds()
				+ "s for active requests to complete");
		for (Connector connector : this.server.getConnectors()) {
			((ServerConnector) connector).setAccepting(false);
		}
		this.shuttingDown = true;
		long end = System.currentTimeMillis() + this.period.toMillis();
		while (System.currentTimeMillis() < end && (this.activeRequests.get() > 0)) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		this.shuttingDown = false;
		long activeRequests = this.activeRequests.get();
		if (activeRequests == 0) {
			logger.info("Graceful shutdown complete");
			return true;
		}
		if (logger.isInfoEnabled()) {
			logger.info("Grace period elaped with " + activeRequests + " request(s) still active");
		}
		return activeRequests == 0;
	}

	@Override
	public boolean isShuttingDown() {
		return this.shuttingDown;
	}

}
