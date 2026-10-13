/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.util.List;

import org.springframework.boot.docker.compose.core.DockerCompose;
import org.springframework.boot.logging.LogLevel;

/**
 * Command used to start Docker Compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public enum StartCommand {

	/**
	 * Start using {@code docker compose up}.
	 */
	UP(DockerCompose::up),

	/**
	 * Start using {@code docker compose start}.
	 */
	START(DockerCompose::start);

	private final Command command;

	StartCommand(Command command) {
		this.command = command;
	}

	void applyTo(DockerCompose dockerCompose, LogLevel logLevel, List<String> arguments) {
		this.command.applyTo(dockerCompose, logLevel, arguments);
	}

	@FunctionalInterface
	private interface Command {

		void applyTo(DockerCompose dockerCompose, LogLevel logLevel, List<String> arguments);

	}

}
