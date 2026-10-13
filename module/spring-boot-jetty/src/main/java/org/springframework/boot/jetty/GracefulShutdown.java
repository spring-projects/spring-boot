/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;

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

	private volatile boolean aborted;

	GracefulShutdown(Server server, Supplier<Integer> activeRequests) {
		this.server = server;
		this.activeRequests = activeRequests;
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		CountDownLatch shutdownInProgress = new CountDownLatch(1);
		new Thread(() -> awaitShutdown(callback, shutdownInProgress), "jetty-shutdown").start();
		try {
			shutdownInProgress.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		for (Connector connector : this.server.getConnectors()) {
			shutdown(connector);
		}
	}

	private void shutdown(Connector connector) {
		connector.shutdown();
	}

	private void awaitShutdown(GracefulShutdownCallback callback, CountDownLatch shutdownInProgress) {
		boolean seenActive = this.activeRequests.get() > 0;
		shutdownInProgress.countDown();
		while (!this.aborted && this.activeRequests.get() > 0) {
			sleep(100);
		}
		if (this.aborted && seenActive) {
			logger.info("Graceful shutdown aborted with one or more requests still active");
			callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
		}
		else {
			logger.info("Graceful shutdown complete");
			callback.shutdownComplete(GracefulShutdownResult.IDLE);
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
		this.aborted = true;
	}

}
