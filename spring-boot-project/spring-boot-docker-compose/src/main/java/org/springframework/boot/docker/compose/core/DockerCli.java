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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.docker.compose.core.DockerCliCommand.Type;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.log.LogMessage;

/**
 * Wrapper around {@code docker} and {@code docker-compose} command line tools.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCli {

	private static final Map<File, DockerCommands> dockerCommandsCache = new HashMap<>();

	private static final Log logger = LogFactory.getLog(DockerCli.class);

	private final ProcessRunner processRunner;

	private final DockerCommands dockerCommands;

	private final DockerComposeFile composeFile;

	private final Set<String> activeProfiles;

	/**
	 * Create a new {@link DockerCli} instance.
	 * @param workingDirectory the working directory or {@code null}
	 * @param composeFile the docker compose file to use
	 * @param activeProfiles the docker compose profiles to activate
	 */
	DockerCli(File workingDirectory, DockerComposeFile composeFile, Set<String> activeProfiles) {
		this.processRunner = new ProcessRunner(workingDirectory);
		this.dockerCommands = dockerCommandsCache.computeIfAbsent(workingDirectory,
				(key) -> new DockerCommands(this.processRunner));
		this.composeFile = composeFile;
		this.activeProfiles = (activeProfiles != null) ? activeProfiles : Collections.emptySet();
	}

	/**
	 * Run the given {@link DockerCli} command and return the response.
	 * @param <R> the response type
	 * @param dockerCommand the command to run
	 * @return the response
	 */
	<R> R run(DockerCliCommand<R> dockerCommand) {
		List<String> command = createCommand(dockerCommand.getType());
		command.addAll(dockerCommand.getCommand());
		Consumer<String> outputConsumer = createOutputConsumer(dockerCommand.getLogLevel());
		String json = this.processRunner.run(outputConsumer, command.toArray(new String[0]));
		return dockerCommand.deserialize(json);
	}

	private Consumer<String> createOutputConsumer(LogLevel logLevel) {
		if (logLevel == null || logLevel == LogLevel.OFF) {
			return null;
		}
		return (line) -> logLevel.log(logger, line);
	}

	private List<String> createCommand(Type type) {
		return switch (type) {
			case DOCKER -> new ArrayList<>(this.dockerCommands.get(type));
			case DOCKER_COMPOSE -> {
				List<String> result = new ArrayList<>(this.dockerCommands.get(type));
				if (this.composeFile != null) {
					result.add("--file");
					result.add(this.composeFile.toString());
				}
				result.add("--ansi");
				result.add("never");
				for (String profile : this.activeProfiles) {
					result.add("--profile");
					result.add(profile);
				}
				yield result;
			}
		};
	}

	/**
	 * Return the {@link DockerComposeFile} being used by this CLI instance.
	 * @return the docker compose file
	 */
	DockerComposeFile getDockerComposeFile() {
		return this.composeFile;
	}

	/**
	 * Holds details of the actual CLI commands to invoke.
	 */
	private static class DockerCommands {

		private final List<String> dockerCommand;

		private final List<String> dockerComposeCommand;

		DockerCommands(ProcessRunner processRunner) {
			this.dockerCommand = getDockerCommand(processRunner);
			this.dockerComposeCommand = getDockerComposeCommand(processRunner);
		}

		private List<String> getDockerCommand(ProcessRunner processRunner) {
			try {
				String version = processRunner.run("docker", "version", "--format", "{{.Client.Version}}");
				logger.trace(LogMessage.format("Using docker %s", version));
				return List.of("docker");
			}
			catch (ProcessStartException ex) {
				throw new DockerProcessStartException("Unable to start docker process. Is docker correctly installed?",
						ex);
			}
			catch (ProcessExitException ex) {
				if (ex.getStdErr().contains("docker daemon is not running")
						|| ex.getStdErr().contains("Cannot connect to the Docker daemon")) {
					throw new DockerNotRunningException(ex.getStdErr(), ex);
				}
				throw ex;
			}
		}

		private List<String> getDockerComposeCommand(ProcessRunner processRunner) {
			try {
				DockerCliComposeVersionResponse response = DockerJson.deserialize(
						processRunner.run("docker", "compose", "version", "--format", "json"),
						DockerCliComposeVersionResponse.class);
				logger.trace(LogMessage.format("Using docker compose %s", response.version()));
				return List.of("docker", "compose");
			}
			catch (ProcessExitException ex) {
				// Ignore and try docker-compose
			}
			try {
				DockerCliComposeVersionResponse response = DockerJson.deserialize(
						processRunner.run("docker-compose", "version", "--format", "json"),
						DockerCliComposeVersionResponse.class);
				logger.trace(LogMessage.format("Using docker-compose %s", response.version()));
				return List.of("docker-compose");
			}
			catch (ProcessStartException ex) {
				throw new DockerProcessStartException(
						"Unable to start 'docker-compose' process or use 'docker compose'. Is docker correctly installed?",
						ex);
			}
		}

		List<String> get(Type type) {
			return switch (type) {
				case DOCKER -> this.dockerCommand;
				case DOCKER_COMPOSE -> this.dockerComposeCommand;
			};
		}

	}

}
