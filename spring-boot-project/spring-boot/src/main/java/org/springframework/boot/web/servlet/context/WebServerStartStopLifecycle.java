/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.servlet.context;

import org.springframework.boot.web.context.WebServerGracefulShutdownLifecycle;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.SmartLifecycle;

/**
 * {@link SmartLifecycle} to start and stop the {@link WebServer} in a
 * {@link ServletWebServerApplicationContext}.
 *
 * @author Andy Wilkinson
 */
class WebServerStartStopLifecycle implements SmartLifecycle {

	private final ServletWebServerApplicationContext applicationContext;

	private final WebServer webServer;

	private volatile boolean running;

	/**
	 * Starts or stops the lifecycle of a web server.
	 * @param applicationContext the application context associated with the web server
	 * @param webServer the web server to start or stop
	 */
	WebServerStartStopLifecycle(ServletWebServerApplicationContext applicationContext, WebServer webServer) {
		this.applicationContext = applicationContext;
		this.webServer = webServer;
	}

	/**
	 * Starts the web server and sets the running flag to true.
	 * @throws Exception if an error occurs while starting the web server
	 */
	@Override
	public void start() {
		this.webServer.start();
		this.running = true;
		this.applicationContext
			.publishEvent(new ServletWebServerInitializedEvent(this.webServer, this.applicationContext));
	}

	/**
	 * Stops the web server and sets the running flag to false.
	 */
	@Override
	public void stop() {
		this.running = false;
		this.webServer.stop();
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
