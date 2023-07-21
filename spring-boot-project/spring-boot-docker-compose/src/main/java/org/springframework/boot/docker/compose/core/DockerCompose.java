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

package org.springframework.boot.docker.compose.core;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.boot.logging.LogLevel;

/**
 * Provides a high-level API to work with Docker compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface DockerCompose {

	/**
	 * Timeout duration used to request a forced stop.
	 */
	Duration FORCE_STOP = Duration.ZERO;

	/**
	 * Run {@code docker compose up} to create and start services. Waits until all
	 * contains are started and healthy.
	 * @param logLevel the log level used to report progress
	 */
	void up(LogLevel logLevel);

	/**
	 * Run {@code docker compose down} to stop and remove any running services.
	 * @param timeout the amount of time to wait or {@link #FORCE_STOP} to stop without
	 * waiting.
	 */
	void down(Duration timeout);

	/**
	 * Run {@code docker compose start} to start services. Waits until all containers are
	 * started and healthy.
	 * @param logLevel the log level used to report progress
	 */
	void start(LogLevel logLevel);

	/**
	 * Run {@code docker compose stop} to stop any running services.
	 * @param timeout the amount of time to wait or {@link #FORCE_STOP} to stop without
	 * waiting.
	 */
	void stop(Duration timeout);

	/**
	 * Return if services have been defined in the {@link DockerComposeFile} for the
	 * active profiles.
	 * @return {@code true} if services have been defined
	 * @see #hasDefinedServices()
	 */
	boolean hasDefinedServices();

	/**
	 * Return the running services for the active profile, or an empty list if no services
	 * are running.
	 * @return the list of running services
	 */
	List<RunningService> getRunningServices();

	/**
	 * Factory method used to create a {@link DockerCompose} instance.
	 * @param file the docker compose file
	 * @param hostname the hostname used for services or {@code null} if the hostname
	 * should be deduced
	 * @param activeProfiles a set of the profiles that should be activated
	 * @return a {@link DockerCompose} instance
	 */
	static DockerCompose get(DockerComposeFile file, String hostname, Set<String> activeProfiles) {
		DockerCli cli = new DockerCli(null, file, activeProfiles);
		return new DefaultDockerCompose(cli, hostname);
	}

}
