/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.reactive.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.SmartLifecycle;

/**
 * {@link SmartLifecycle} to start and stop the {@link WebServer} in a
 * {@link ReactiveWebServerApplicationContext}.
 *
 * @author Andy Wilkinson
 */
class WebServerStartStopLifecycle implements SmartLifecycle {

	private final WebServerManager weServerManager;

	private volatile boolean running;

	WebServerStartStopLifecycle(WebServerManager weServerManager) {
		this.weServerManager = weServerManager;
	}

	@Override
	public void start() {
		this.weServerManager.start();
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
		this.weServerManager.stop();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return WebServerApplicationContext.START_STOP_LIFECYCLE_PHASE;
	}

	@Override
	public boolean isPauseable() {
		return false;
	}

}
