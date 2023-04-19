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

package org.springframework.boot.docker.compose.lifecycle;

/**
 * Docker compose lifecycle management.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public enum LifecycleManagement {

	/**
	 * Don't start or stop docker compose.
	 */
	NONE(false, false),

	/**
	 * Only start docker compose if it's not running.
	 */
	START_ONLY(true, false),

	/**
	 * Start and stop docker compose if it's not running.
	 */
	START_AND_STOP(true, true);

	private final boolean startup;

	private final boolean shutdown;

	LifecycleManagement(boolean startup, boolean shutdown) {
		this.startup = startup;
		this.shutdown = shutdown;
	}

	/**
	 * Return whether docker compose should be started.
	 * @return whether docker compose should be started.
	 */
	boolean shouldStartup() {
		return this.startup;
	}

	/**
	 * Return whether docker compose should be stopped.
	 * @return whether docker compose should be stopped
	 */
	boolean shouldShutdown() {
		return this.shutdown;
	}

}
