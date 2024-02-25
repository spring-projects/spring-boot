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

import java.util.function.BiConsumer;

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

	private final BiConsumer<DockerCompose, LogLevel> action;

	/**
	 * Sets the action to be performed when the StartCommand is executed.
	 * @param action the action to be performed, which takes a DockerCompose object and a
	 * LogLevel object as parameters
	 */
	StartCommand(BiConsumer<DockerCompose, LogLevel> action) {
		this.action = action;
	}

	/**
	 * Applies the specified log level to the given DockerCompose instance.
	 * @param dockerCompose the DockerCompose instance to apply the log level to
	 * @param logLevel the log level to be applied
	 */
	void applyTo(DockerCompose dockerCompose, LogLevel logLevel) {
		this.action.accept(dockerCompose, logLevel);
	}

}
