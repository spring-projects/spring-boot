/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.docker.compose.core.DockerCompose;

/**
 * Command used to stop Docker Compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public enum StopCommand {

	/**
	 * Stop using {@code docker compose down}.
	 */
	DOWN(DockerCompose::down),

	/**
	 * Stop using {@code docker compose stop}.
	 */
	STOP(DockerCompose::stop);

	private final Command command;

	StopCommand(Command command) {
		this.command = command;
	}

	void applyTo(DockerCompose dockerCompose, Duration timeout, List<String> arguments) {
		this.command.applyTo(dockerCompose, timeout, arguments);
	}

	@FunctionalInterface
	private interface Command {

		void applyTo(DockerCompose dockerCompose, Duration timeout, List<String> arguments);

	}

}
