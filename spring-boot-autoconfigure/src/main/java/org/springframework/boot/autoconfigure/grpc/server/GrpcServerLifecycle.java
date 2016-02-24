/*
 * Copyright 2016-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.grpc.server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.Server;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;

/**
 * Manages the lifecycle of a gRPC server. It uses the {@link GrpcServerFactory}
 * to create a new instance of a gRPC server, and then manages it according to the
 * {@link SmartLifecycle}.
 * @author Ray Tsang
 */
public class GrpcServerLifecycle implements SmartLifecycle {
	private static final Log logger = LogFactory
			.getLog(GrpcServerLifecycle.class);
	private static AtomicInteger serverCounter = new AtomicInteger(-1);

	private volatile Server server;
	private volatile int phase = Integer.MAX_VALUE;
	private final GrpcServerFactory factory;

	public GrpcServerLifecycle(GrpcServerFactory factory) {
		this.factory = factory;
	}

	@Override
	public void start() {
		try {
			createAndStartGrpcServer();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void stop() {
		stopAndReleaseGrpcServer();
	}

	@Override
	public boolean isRunning() {
		return this.server == null ? false : !this.server.isShutdown();
	}

	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	protected void createAndStartGrpcServer() throws IOException {
		Server localServer = this.server;
		if (localServer == null) {
			this.server = this.factory.createServer();
			this.server.start();
			logger.info("gRPC Server started, listening on address: "
					+ this.factory.getAddress() + ", port: " + this.factory.getPort());

			Thread awaitThread = new Thread(
					"container-" + (serverCounter.incrementAndGet())) {

				@Override
				public void run() {
					try {
						GrpcServerLifecycle.this.server.awaitTermination();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}

			};
			awaitThread.setDaemon(false);
			awaitThread.start();
		}
	}

	protected void stopAndReleaseGrpcServer() {
		Server localServer = this.server;
		if (localServer != null) {
			localServer.shutdown();
			this.server = null;
			logger.info("gRPC server stopped");
		}
	}

}
