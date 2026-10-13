/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.context.SmartLifecycle;

/**
 * {@link SmartLifecycle} to trigger {@link WebServer} graceful shutdown.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public final class WebServerGracefulShutdownLifecycle implements SmartLifecycle {

	private final WebServer webServer;

	private volatile boolean running;

	/**
	 * Creates a new {@code WebServerGracefulShutdownLifecycle} that will gracefully shut
	 * down the given {@code webServer}.
	 * @param webServer web server to shut down gracefully
	 */
	public WebServerGracefulShutdownLifecycle(WebServer webServer) {
		this.webServer = webServer;
	}

	@Override
	public void start() {
		this.running = true;
	}

	@Override
	public void stop() {
		throw new UnsupportedOperationException("Stop must not be invoked directly");
	}

	@Override
	public void stop(Runnable callback) {
		this.running = false;
		this.webServer.shutDownGracefully((result) -> callback.run());
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return WebServerApplicationContext.GRACEFUL_SHUTDOWN_PHASE;
	}

	@Override
	public boolean isPauseable() {
		return false;
	}

}
