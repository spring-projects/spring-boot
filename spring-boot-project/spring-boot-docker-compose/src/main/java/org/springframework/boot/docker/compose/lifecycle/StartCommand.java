/*
 * Copyright 2012-2024 the original author or authors.
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
