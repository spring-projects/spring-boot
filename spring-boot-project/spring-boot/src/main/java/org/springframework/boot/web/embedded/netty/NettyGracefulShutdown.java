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

import org.springframework.boot.web.server.GracefulShutdown;

/**
 * {@link GracefulShutdown} for a Reactor Netty {@link DisposableServer}.
 *
 * @author Andy Wilkinson
 */
final class NettyGracefulShutdown implements GracefulShutdown {

	private static final Log logger = LogFactory.getLog(NettyGracefulShutdown.class);

	private final Supplier<DisposableServer> disposableServer;

	private final Duration period;

	private volatile boolean shuttingDown;

	NettyGracefulShutdown(Supplier<DisposableServer> disposableServer, Duration period) {
		this.disposableServer = disposableServer;
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
		this.shuttingDown = true;
		try {
			disposeNow(server);
			logger.info("Graceful shutdown complete");
			return true;
		}
		catch (IllegalStateException ex) {
			logger.info("Grace period elapsed with one ore more requests still active");
			return false;
		}
		finally {
			this.shuttingDown = false;
		}
	}

	private void disposeNow(DisposableServer server) {
		if (this.period != null) {
			server.disposeNow(this.period);
		}
		else {
			server.disposeNow();
		}
	}

	@Override
	public boolean isShuttingDown() {
		return this.shuttingDown;
	}

}
