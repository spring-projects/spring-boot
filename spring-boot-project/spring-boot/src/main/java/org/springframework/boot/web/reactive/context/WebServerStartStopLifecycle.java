/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.boot.web.server.WebServer;
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

	/**
	 * Starts or stops the lifecycle of a web server.
	 * @param weServerManager the web server manager responsible for starting or stopping
	 * the web server
	 */
	WebServerStartStopLifecycle(WebServerManager weServerManager) {
		this.weServerManager = weServerManager;
	}

	/**
	 * Starts the web server.
	 *
	 * This method starts the web server by calling the start() method of the
	 * weServerManager object. It also sets the running flag to true.
	 */
	@Override
	public void start() {
		this.weServerManager.start();
		this.running = true;
	}

	/**
	 * Stops the web server.
	 *
	 * This method sets the 'running' flag to false and stops the web server manager.
	 */
	@Override
	public void stop() {
		this.running = false;
		this.weServerManager.stop();
	}

	/**
	 * Returns a boolean value indicating whether the web server is currently running.
	 * @return true if the web server is running, false otherwise
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Returns the phase of the lifecycle for graceful shutdown of the web server. The
	 * phase is calculated by subtracting 1024 from the phase of the smart lifecycle.
	 * @return the phase of the graceful shutdown lifecycle
	 */
	@Override
	public int getPhase() {
		return WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 1024;
	}

}
