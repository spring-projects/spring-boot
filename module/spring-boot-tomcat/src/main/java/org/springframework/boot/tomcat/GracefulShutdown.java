/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.catalina.Container;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.server.GracefulShutdownCallback;
import org.springframework.boot.web.server.GracefulShutdownResult;

/**
 * Handles Tomcat graceful shutdown.
 *
 * @author Andy Wilkinson
 */
final class GracefulShutdown {

	private static final Log logger = LogFactory.getLog(GracefulShutdown.class);

	private final Tomcat tomcat;

	private volatile boolean aborted;

	GracefulShutdown(Tomcat tomcat) {
		this.tomcat = tomcat;
	}

	void shutDownGracefully(GracefulShutdownCallback callback) {
		logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
		CountDownLatch shutdownUnderway = new CountDownLatch(1);
		new Thread(() -> doShutdown(callback, shutdownUnderway), "tomcat-shutdown").start();
		try {
			shutdownUnderway.await();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void doShutdown(GracefulShutdownCallback callback, CountDownLatch shutdownUnderway) {
		try {
			List<Connector> connectors = getConnectors();
			connectors.forEach(this::close);
			GracefulShutdownResult result = awaitInactiveOrAborted(shutdownUnderway);
			if (result == GracefulShutdownResult.REQUESTS_ACTIVE) {
				logger.info("Graceful shutdown aborted with one or more requests still active");
			}
			else {
				logger.info("Graceful shutdown complete");
			}
			callback.shutdownComplete(result);
		}
		finally {
			shutdownUnderway.countDown();
		}
	}

	private List<Connector> getConnectors() {
		List<Connector> connectors = new ArrayList<>();
		for (Service service : this.tomcat.getServer().findServices()) {
			Collections.addAll(connectors, service.findConnectors());
		}
		return connectors;
	}

	private void close(Connector connector) {
		connector.pause();
		connector.getProtocolHandler().closeServerSocketGraceful();
	}

	private GracefulShutdownResult awaitInactiveOrAborted(CountDownLatch shutdownUnderway) {
		boolean active = isActive();
		shutdownUnderway.countDown();
		try {
			while (!this.aborted && isActive()) {
				Thread.sleep(50);
			}
			if (this.aborted && active) {
				return GracefulShutdownResult.REQUESTS_ACTIVE;
			}
			return GracefulShutdownResult.IDLE;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return GracefulShutdownResult.REQUESTS_ACTIVE;
		}
	}

	private boolean isActive() {
		for (Container host : this.tomcat.getEngine().findChildren()) {
			for (Container context : host.findChildren()) {
				if (isActive(context)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isActive(Container context) {
		try {
			if (((StandardContext) context).getInProgressAsyncCount() > 0) {
				return true;
			}
			for (Container wrapper : context.findChildren()) {
				if (((StandardWrapper) wrapper).getCountAllocated() > 0) {
					return true;
				}
			}
			return false;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	void abort() {
		this.aborted = true;
	}

}
