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

package org.springframework.boot.web.embedded.undertow;

import java.time.Duration;

import io.undertow.server.handlers.GracefulShutdownHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.server.GracefulShutdown;

/**
 * {@link GracefulShutdown} for Undertow.
 *
 * @author Andy Wilkinson
 */
class UndertowGracefulShutdown implements GracefulShutdown {

	private static final Log logger = LogFactory.getLog(UndertowGracefulShutdown.class);

	private final GracefulShutdownHandler gracefulShutdownHandler;

	private final Duration period;

	private volatile boolean shuttingDown;

	UndertowGracefulShutdown(GracefulShutdownHandler gracefulShutdownHandler, Duration period) {
		this.gracefulShutdownHandler = gracefulShutdownHandler;
		this.period = period;
	}

	@Override
	public boolean shutDownGracefully() {
		logger.info("Commencing graceful shutdown, allowing up to " + this.period.getSeconds()
				+ "s for active requests to complete");
		this.gracefulShutdownHandler.shutdown();
		this.shuttingDown = true;
		boolean graceful = false;
		try {
			graceful = this.gracefulShutdownHandler.awaitShutdown(this.period.toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		finally {
			this.shuttingDown = false;
		}
		if (graceful) {
			logger.info("Graceful shutdown complete");
			return true;
		}
		logger.info("Grace period elapsed with one or more requests still active");
		return graceful;
	}

	@Override
	public boolean isShuttingDown() {
		return this.shuttingDown;
	}

}
