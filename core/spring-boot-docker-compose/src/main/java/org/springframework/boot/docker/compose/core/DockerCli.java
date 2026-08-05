/*
 * Copyright 2012-present the original author or authors.
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
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeVersion;
import org.springframework.boot.docker.compose.core.DockerCliCommand.Type;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.log.LogMessage;
import org.springframework.util.CollectionUtils;

/**
 * Wrapper around {@code docker} and {@code docker-compose} command line tools. Podman is
 * supported as an alternative to Docker, see {@link ContainerEngine}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCli {

	private static final Map<@Nullable File, DockerCommands> dockerCommandsCache = new HashMap<>();

	private static final Log logger = LogFactory.getLog(DockerCli.class);

	private final ProcessRunner processRunner;

	private final DockerCommands dockerCommands;

	private final DockerComposeOptions dockerComposeOptions;

	private final ComposeVersion composeVersion;

	/**
	 * Create a new {@link DockerCli} instance.
	 * @param workingDirectory the working directory or {@code null}
	 * @param dockerComposeOptions the Docker Compose options to use or {@code null}.
	 */
	DockerCli(@Nullable File workingDirectory, @Nullable DockerComposeOptions dockerComposeOptions) {
		this.processRunner = new ProcessRunner(workingDirectory);
		this.dockerCommands = dockerCommandsCache.computeIfAbsent(workingDirectory,
				(key) -> new DockerCommands(this.processRunner));
		this.dockerComposeOptions = (dockerComposeOptions != null) ? dockerComposeOptions : DockerComposeOptions.none();
		this.composeVersion = ComposeVersion.of(this.dockerCommands.get(Type.DOCKER_COMPOSE).version());
	}

	/**
	 * Run the given {@link DockerCli} command and return the response.
	 * @param <R> the response type
	 * @param dockerCommand the command to run
	 * @return the response
	 */
	<R> R run(DockerCliCommand<R> dockerCommand) {
		List<String> command = createCommand(dockerCommand.getType());
		command.addAll(dockerCommand.getCommand(this.composeVersion));
		Consumer<String> outputConsumer = createOutputConsumer(dockerCommand.getLogLevel());
		String response = this.processRunner.run(outputConsumer, command.toArray(new String[0]));
		return dockerCommand.convert(response);
	}

	private @Nullable Consumer<String> createOutputConsumer(@Nullable LogLevel logLevel) {
		if (logLevel == null || logLevel == LogLevel.OFF) {
			return null;
		}
		return (line) -> logLevel.log(logger, line);
	}

	private List<String> createCommand(Type type) {
		return switch (type) {
			case DOCKER -> new ArrayList<>(this.dockerCommands.get(type).command());
			case DOCKER_COMPOSE -> {
				List<String> result = new ArrayList<>(this.dockerCommands.get(type).command());
				DockerComposeFile composeFile = this.dockerComposeOptions.composeFile();
				if (composeFile != null) {
					for (File file : composeFile.getFiles()) {
						result.add("--file");
						result.add(file.getPath());
					}
				}
				result.add("--ansi");
				result.add("never");
				Set<String> activeProfiles = this.dockerComposeOptions.activeProfiles();
				if (!CollectionUtils.isEmpty(activeProfiles)) {
					for (String profile : activeProfiles) {
						result.add("--profile");
						result.add(profile);
					}
				}
				List<String> arguments = this.dockerComposeOptions.arguments();
				if (!CollectionUtils.isEmpty(arguments)) {
					result.addAll(arguments);
				}
				yield result;
			}
		};
	}

	/**
	 * Return the {@link DockerComposeFile} being used by this CLI instance.
	 * @return the Docker Compose file
	 */
	@Nullable DockerComposeFile getDockerComposeFile() {
		return this.dockerComposeOptions.composeFile();
	}

	/**
	 * Holds details of the actual CLI commands to invoke.
	 */
	static class DockerCommands {

		private final DockerCommand dockerCommand;

		private final DockerCommand dockerComposeCommand;

		DockerCommands(ProcessRunner processRunner) {
			DetectedContainerEngine detected = getContainerEngine(processRunner);
			this.dockerCommand = new DockerCommand(detected.version(), List.of(detected.engine().command()));
			this.dockerComposeCommand = getDockerComposeCommand(processRunner, detected.engine());
		}

		private static DetectedContainerEngine getContainerEngine(ProcessRunner processRunner) {
			List<RuntimeException> failures = new ArrayList<>();
			for (ContainerEngine candidate : ContainerEngine.values()) {
				try {
					String version = processRunner.run(candidate.command(), "version", "--format",
							"{{.Client.Version}}");
					logger.trace(LogMessage.format("Using %s %s", candidate.command(), version));
					return new DetectedContainerEngine(candidate, version);
				}
				catch (ProcessStartException ex) {
					// Ignore and try the next candidate
					failures.add(ex);
				}
				catch (ProcessExitException ex) {
					if (candidate.isNotRunning(ex.getStdErr())) {
						throw new DockerNotRunningException(ex.getStdErr(), ex);
					}
					throw ex;
				}
			}
			throw startException("Unable to start docker or podman process. Is docker or podman correctly installed?",
					failures);
		}

		private static DockerCommand getDockerComposeCommand(ProcessRunner processRunner,
				ContainerEngine containerEngine) {
			List<RuntimeException> failures = new ArrayList<>();
			for (List<String> command : containerEngine.composeCommands()) {
				try {
					DockerCliComposeVersionResponse response = DockerJson.deserialize(
							processRunner.run(join(command, List.of("version", "--format", "json"))),
							DockerCliComposeVersionResponse.class);
					logger.trace(LogMessage.format("Using %s %s", String.join(" ", command), response.version()));
					return new DockerCommand(response.version(), command);
				}
				catch (ProcessStartException | ProcessExitException | DockerOutputParseException ex) {
					// Ignore and try the next candidate
					failures.add(ex);
				}
			}
			throw startException("Unable to use %s. Is %s correctly installed?"
				.formatted(containerEngine.describeComposeCommands(), containerEngine.command()), failures);
		}

		private static String[] join(List<String> command, List<String> arguments) {
			List<String> result = new ArrayList<>(command);
			result.addAll(arguments);
			return result.toArray(String[]::new);
		}

		private static DockerProcessStartException startException(String message, List<RuntimeException> failures) {
			DockerProcessStartException result = new DockerProcessStartException(message, failures.get(0));
			failures.subList(1, failures.size()).forEach(result::addSuppressed);
			return result;
		}

		DockerCommand get(Type type) {
			return switch (type) {
				case DOCKER -> this.dockerCommand;
				case DOCKER_COMPOSE -> this.dockerComposeCommand;
			};
		}

		/**
		 * A {@link ContainerEngine} that has been found on the local machine.
		 *
		 * @param engine the container engine
		 * @param version the reported client version
		 */
		private record DetectedContainerEngine(ContainerEngine engine, String version) {

		}

	}

	/**
	 * Container engines that can be used to run the commands, in the order that they are
	 * tried.
	 */
	enum ContainerEngine {

		/**
		 * Docker, using either the {@code docker compose} plugin or the standalone
		 * {@code docker-compose} binary.
		 */
		DOCKER("docker", List.of(List.of("docker", "compose"), List.of("docker-compose"))) {

			@Override
			boolean isNotRunning(String stdErr) {
				return stdErr.contains("docker daemon is not running")
						|| stdErr.contains("Cannot connect to the Docker daemon");
			}

		},

		/**
		 * Podman. Only {@code podman compose} is used since it delegates to an external
		 * Docker Compose compatible provider. The {@code podman-compose} binary is not
		 * supported as its command line arguments and output are not compatible with
		 * Docker Compose.
		 */
		PODMAN("podman", List.of(List.of("podman", "compose"))) {

			@Override
			boolean isNotRunning(String stdErr) {
				return stdErr.contains("Cannot connect to Podman");
			}

		};

		private final String command;

		private final List<List<String>> composeCommands;

		ContainerEngine(String command, List<List<String>> composeCommands) {
			this.command = command;
			this.composeCommands = composeCommands;
		}

		String command() {
			return this.command;
		}

		List<List<String>> composeCommands() {
			return this.composeCommands;
		}

		String describeComposeCommands() {
			return this.composeCommands.stream()
				.map((command) -> "'%s'".formatted(String.join(" ", command)))
				.collect(Collectors.joining(" or "));
		}

		/**
		 * Return if the given standard error output indicates that the engine is
		 * installed, but not running.
		 * @param stdErr the standard error output
		 * @return {@code true} if the engine is not running
		 */
		abstract boolean isNotRunning(String stdErr);

	}

	record DockerCommand(String version, List<String> command) {

	}

	/**
	 * Options for Docker Compose.
	 *
	 * @param composeFile the Docker Compose file to use
	 * @param activeProfiles the profiles to activate
	 * @param arguments the arguments to pass to Docker Compose
	 */
	record DockerComposeOptions(@Nullable DockerComposeFile composeFile, Set<String> activeProfiles,
			List<String> arguments) {

		DockerComposeOptions(@Nullable DockerComposeFile composeFile, @Nullable Set<String> activeProfiles,
				@Nullable List<String> arguments) {
			this.composeFile = composeFile;
			this.activeProfiles = (activeProfiles != null) ? activeProfiles : Collections.emptySet();
			this.arguments = (arguments != null) ? arguments : Collections.emptyList();
		}

		static DockerComposeOptions none() {
			return new DockerComposeOptions(null, null, null);
		}

	}

}
