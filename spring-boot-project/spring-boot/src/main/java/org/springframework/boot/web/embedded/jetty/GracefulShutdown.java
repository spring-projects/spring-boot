/*
 * Copyright 2012-2024 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.core.log.LogMessage;
import org.springframework.util.ReflectionUtils;

/**
 * Handles Jetty graceful shutdown.
 *
 * @author Andy Wilkinson
 * @author Onur Kagan Ozcan
 */
final class GracefulShutdown {

	private static final Log logger = LogFactory.getLog(GracefulShutdown.class);

	private final Server server;

	private final Supplier<Integer> activeRequests;

	private volatile boolean shuttingDown = false;

	/**
     * Gracefully shuts down the server by stopping the server and waiting for all active requests to complete.
     * 
     * @param server the server to be shut down
     * @param activeRequests a supplier that provides the number of active requests
     */
    GracefulShutdown(Server server, Supplier<Integer> activeRequests) {
		this.server = server;
		this.activeRequests = activeRequests;
	}

	/**
     * Initiates a graceful shutdown of the server.
     * 
     * @param callback the callback function to be executed after the shutdown is complete
     */
    void shutDownGracefully(GracefulShutdownCallback callback) {
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		boolean jetty10 = isJetty10();
		for (Connector connector : this.server.getConnectors()) {
			shutdown(connector, !jetty10);
		}
		this.shuttingDown = true;
		new Thread(() -> awaitShutdown(callback), "jetty-shutdown").start();

	}

	/**
     * Shuts down the given Connector and optionally waits for the result.
     * 
     * @param connector the Connector to be shutdown
     * @param getResult flag indicating whether to wait for the shutdown result
     */
    @SuppressWarnings("unchecked")
	private void shutdown(Connector connector, boolean getResult) {
		Future<Void> result;
		try {
			result = connector.shutdown();
		}
		catch (NoSuchMethodError ex) {
			Method shutdown = ReflectionUtils.findMethod(connector.getClass(), "shutdown");
			result = (Future<Void>) ReflectionUtils.invokeMethod(shutdown, connector);
		}
		if (getResult) {
			try {
				result.get();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (ExecutionException ex) {
				// Continue
			}
		}
	}

	/**
     * Checks if the Jetty version is 10 or higher.
     * 
     * @return true if the Jetty version is 10 or higher, false otherwise.
     */
    private boolean isJetty10() {
		try {
			return CompletableFuture.class.equals(Connector.class.getMethod("shutdown").getReturnType());
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
     * Waits for the shutdown process to complete gracefully.
     * 
     * @param callback the callback to be invoked when the shutdown process is complete
     */
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
			logger.info(LogMessage.format("Graceful shutdown aborted with %d request%s still active", activeRequests,
					(activeRequests == 1) ? "" : "s"));
			callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
		}
	}

	/**
     * Suspends the execution of the current thread for the specified number of milliseconds.
     * 
     * @param millis the number of milliseconds to sleep
     * @throws IllegalArgumentException if the value of millis is negative
     * @throws InterruptedException if the current thread is interrupted while sleeping
     */
    private void sleep(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/**
     * Aborts the graceful shutdown process.
     * This method sets the shuttingDown flag to false, indicating that the shutdown process should be stopped.
     */
    void abort() {
		this.shuttingDown = false;
	}

}
