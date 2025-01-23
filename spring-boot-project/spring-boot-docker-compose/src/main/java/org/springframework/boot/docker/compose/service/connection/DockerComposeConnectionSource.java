/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.core.env.Environment;

/**
 * Passed to {@link DockerComposeConnectionDetailsFactory} to provide details of the
 * {@link RunningService running Docker Compose service}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see DockerComposeConnectionDetailsFactory
 */
public final class DockerComposeConnectionSource {

	private final RunningService runningService;

	private final Environment environment;

	/**
	 * Create a new {@link DockerComposeConnectionSource} instance.
	 * @param runningService the running Docker Compose service
	 * @param environment environment in which the current application is running
	 */
	DockerComposeConnectionSource(RunningService runningService, Environment environment) {
		this.runningService = runningService;
		this.environment = environment;
	}

	/**
	 * Return the running Docker Compose service.
	 * @return the running service
	 */
	public RunningService getRunningService() {
		return this.runningService;
	}

	/**
	 * Environment in which the current application is running.
	 * @return the environment
	 * @since 3.5.0
	 */
	public Environment getEnvironment() {
		return this.environment;
	}

}
