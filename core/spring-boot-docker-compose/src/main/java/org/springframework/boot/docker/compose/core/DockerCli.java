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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeContext;
import org.springframework.boot.docker.compose.core.DockerCliCommand.ComposeVersion;
import org.springframework.boot.docker.compose.core.DockerCliCommand.Type;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.log.LogMessage;
import org.springframework.util.CollectionUtils;

/**
 * Wrapper around {@code docker}, {@code docker-compose}, {@code podman}, and
 * {@code podman-compose} command line tools.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerCli {

	private static final Map<@Nullable File, DockerCommands> dockerCommandsCache = new HashMap<>();

	private static final Log logger = LogFactory.getLog(DockerCli.class);

	private static final Pattern PODMAN_COMPOSE_VERSION_PATTERN = Pattern
		.compile("podman-compose version (\\d+\\.\\d+\\.\\d+)");

	private final ProcessRunner processRunner;

	private final DockerCommands dockerCommands;

	private final DockerComposeOptions dockerComposeOptions;

	private final ComposeContext composeContext;

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
		ComposeVersion composeVersion = ComposeVersion.of(this.dockerCommands.get(Type.DOCKER_COMPOSE).version());
		this.composeContext = new ComposeContext(this.dockerCommands.engine(), composeVersion);
	}

	DockerCli(ProcessRunner processRunner) {
		this.processRunner = processRunner;
		this.dockerCommands = new DockerCommands(this.processRunner);
		this.dockerComposeOptions = DockerComposeOptions.none();
		ComposeVersion composeVersion = ComposeVersion.of(this.dockerCommands.get(Type.DOCKER_COMPOSE).version());
		this.composeContext = new ComposeContext(this.dockerCommands.engine(), composeVersion);
	}

	<R> R run(DockerCliCommand<R> dockerCommand) {
		if (!dockerCommand.isSupported(this.composeContext)) {
			return dockerCommand.emptyResponse();
		}
		List<String> command = createCommand(dockerCommand.getType());
		command.addAll(dockerCommand.getCommand(this.composeContext));
		Consumer<String> outputConsumer = createOutputConsumer(dockerCommand.getLogLevel());
		String response = this.processRunner.run(outputConsumer, command.toArray(new String[0]));
		return dockerCommand.convert(response, this.composeContext);
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
				DockerCommand composeCmd = this.dockerCommands.get(type);
				List<String> result = new ArrayList<>(composeCmd.command());
				DockerComposeFile composeFile = this.dockerComposeOptions.composeFile();
				if (composeFile != null) {
					for (File file : composeFile.getFiles()) {
						result.add("--file");
						result.add(file.getPath());
					}
				}

				if (composeCmd.supportsAnsi()) {
					result.add("--ansi");
					result.add("never");
				}

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
	private static class DockerCommands {

		private final ContainerEngine engine;

		private final DockerCommand dockerCommand;

		private final DockerCommand dockerComposeCommand;

		DockerCommands(ProcessRunner processRunner) {
			Provider provider = discoverProvider(processRunner);
			this.engine = provider.engine();
			this.dockerCommand = provider.engineCommand();
			this.dockerComposeCommand = provider.composeCommand();
		}

		ContainerEngine engine() {
			return this.engine;
		}

		private Provider discoverProvider(ProcessRunner processRunner) {
			try {
				return getDockerProvider(processRunner);
			}
			catch (ProcessStartException ex1) {
				// Only fall back to Podman if 'docker' executable is completely missing
				// on PATH
				try {
					return getPodmanProvider(processRunner);
				}
				catch (ProcessStartException ex2) {
					DockerProcessStartException exception = new DockerProcessStartException(
							"Unable to find 'docker' or 'podman' executable on PATH.", ex1);
					exception.addSuppressed(ex2);
					throw exception;
				}
			}
		}

		private Provider getDockerProvider(ProcessRunner processRunner) {
			DockerCommand engine = fetchEngineCommand(processRunner, "docker");
			try {
				return new Provider(ContainerEngine.DOCKER, engine, fetchDockerComposePlugin(processRunner));
			}
			catch (ProcessStartException | ProcessExitException ex1) {
				try {
					return new Provider(ContainerEngine.DOCKER, engine, fetchDockerComposeStandalone(processRunner));
				}
				catch (ProcessStartException | ProcessExitException ex2) {
					throw new DockerProcessStartException(
							"Docker binary was found, but neither 'docker compose' nor 'docker-compose' could be executed.",
							ex1);
				}
			}
		}

		private Provider getPodmanProvider(ProcessRunner processRunner) {
			DockerCommand engine = fetchEngineCommand(processRunner, "podman");
			try {
				return new Provider(ContainerEngine.PODMAN, engine, fetchPodmanComposePlugin(processRunner));
			}
			catch (ProcessStartException | ProcessExitException ex1) {
				try {
					return new Provider(ContainerEngine.PODMAN, engine, fetchPodmanComposeStandalone(processRunner));
				}
				catch (ProcessStartException | ProcessExitException ex2) {
					throw new DockerProcessStartException(
							"Podman binary was found, but neither 'podman compose' nor 'podman-compose' could be executed.",
							ex1);
				}
			}
		}

		private DockerCommand fetchEngineCommand(ProcessRunner processRunner, String executable) {
			try {
				String version = processRunner.run(executable, "version", "--format", "{{.Client.Version}}");
				logger.trace(LogMessage.format("Using %s %s", executable, version.trim()));
				return new DockerCommand(version.trim(), List.of(executable), false);
			}
			catch (ProcessExitException ex) {
				if ("docker".equals(executable) && isDaemonNotRunning(ex.getStdErr())) {
					throw new DockerNotRunningException(ex.getStdErr(), ex);
				}
				throw new DockerProcessStartException(executable + " process failed to run correctly", ex);
			}
		}

		private DockerCommand fetchDockerComposePlugin(ProcessRunner processRunner) {
			String output = processRunner.run("docker", "compose", "version", "--format", "json");
			DockerCliComposeVersionResponse response = DockerJson.deserialize(output,
					DockerCliComposeVersionResponse.class);
			logger.trace(LogMessage.format("Using docker compose %s", response.version()));
			return new DockerCommand(response.version(), List.of("docker", "compose"), true);
		}

		private DockerCommand fetchDockerComposeStandalone(ProcessRunner processRunner) {
			String output = processRunner.run("docker-compose", "version", "--format", "json");
			DockerCliComposeVersionResponse response = DockerJson.deserialize(output,
					DockerCliComposeVersionResponse.class);
			logger.trace(LogMessage.format("Using docker-compose %s", response.version()));
			return new DockerCommand(response.version(), List.of("docker-compose"), true);
		}

		private DockerCommand fetchPodmanComposePlugin(ProcessRunner processRunner) {
			String output = processRunner.run("podman", "compose", "version");
			String version = parsePodmanComposeVersion(output);
			logger.trace(LogMessage.format("Using podman compose %s", version));
			return new DockerCommand(version, List.of("podman", "compose"), false);
		}

		private DockerCommand fetchPodmanComposeStandalone(ProcessRunner processRunner) {
			String output = processRunner.run("podman-compose", "version");
			String version = parsePodmanComposeVersion(output);
			logger.trace(LogMessage.format("Using podman-compose %s", version));
			return new DockerCommand(version, List.of("podman-compose"), false);
		}

		private static String parsePodmanComposeVersion(String rawOutput) {
			Matcher matcher = PODMAN_COMPOSE_VERSION_PATTERN.matcher(rawOutput);
			if (matcher.find()) {
				return matcher.group(1);
			}
			throw new IllegalStateException("Unable to parse version from podman-compose output: " + rawOutput);
		}

		private static boolean isDaemonNotRunning(String stdErr) {
			return stdErr.contains("docker daemon is not running")
					|| stdErr.contains("Cannot connect to the Docker daemon");
		}

		DockerCommand get(Type type) {
			return switch (type) {
				case DOCKER -> this.dockerCommand;
				case DOCKER_COMPOSE -> this.dockerComposeCommand;
			};
		}

	}

	private record Provider(ContainerEngine engine, DockerCommand engineCommand, DockerCommand composeCommand) {

	}

	private record DockerCommand(String version, List<String> command, boolean supportsAnsi) {

	}

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
