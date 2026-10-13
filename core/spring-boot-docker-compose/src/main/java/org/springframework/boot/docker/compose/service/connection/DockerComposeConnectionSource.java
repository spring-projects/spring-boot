/*
 * Copyright 2012-present the original author or authors.
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
