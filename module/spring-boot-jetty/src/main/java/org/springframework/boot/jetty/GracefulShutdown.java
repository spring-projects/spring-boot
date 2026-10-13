/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.util.Assert;
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
		boolean jetty10 = isJetty10();
		for (Connector connector : this.server.getConnectors()) {
			shutdown(connector, !jetty10);
		}

	}

	@SuppressWarnings("unchecked")
	private void shutdown(Connector connector, boolean getResult) {
		Future<Void> result;
		try {
			result = connector.shutdown();
		}
		catch (NoSuchMethodError ex) {
			Method shutdown = ReflectionUtils.findMethod(connector.getClass(), "shutdown");
			Assert.state(shutdown != null, "'shutdown' must not be null");
			result = (Future<Void>) ReflectionUtils.invokeMethod(shutdown, connector);
		}
		if (getResult) {
			try {
				Assert.state(result != null, "'result' must not be null");
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

	private boolean isJetty10() {
		try {
			return CompletableFuture.class.equals(Connector.class.getMethod("shutdown").getReturnType());
		}
		catch (Exception ex) {
			return false;
		}
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
