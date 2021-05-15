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
 * {@link SmartLifecycle} to trigger {@link WebServer} graceful shutdown in a
 * {@link ReactiveWebServerApplicationContext}.
 *
 * @author Andy Wilkinson
 */
class WebServerGracefulShutdownLifecycle implements SmartLifecycle {

	private final WebServerManager serverManager;

	private volatile boolean running;

	WebServerGracefulShutdownLifecycle(WebServerManager serverManager) {
		this.serverManager = serverManager;
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
		this.serverManager.shutDownGracefully(callback);
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
