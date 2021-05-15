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

package org.springframework.boot.web.reactive.context;

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
		return Integer.MAX_VALUE - 1;
	}

}
