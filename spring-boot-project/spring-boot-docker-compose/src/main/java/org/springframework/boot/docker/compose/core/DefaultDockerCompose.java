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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.logging.LogLevel;
import org.springframework.util.Assert;

/**
 * Default {@link DockerCompose} implementation backed by {@link DockerCli}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultDockerCompose implements DockerCompose {

	private final DockerCli cli;

	private final DockerHost hostname;

	/**
     * Constructs a new DefaultDockerCompose object with the specified DockerCli and host.
     * 
     * @param cli the DockerCli object used to interact with Docker
     * @param host the hostname of the Docker host
     */
    DefaultDockerCompose(DockerCli cli, String host) {
		this.cli = cli;
		this.hostname = DockerHost.get(host, () -> cli.run(new DockerCliCommand.Context()));
	}

	/**
     * Starts the Docker Compose services.
     * 
     * @param logLevel the log level to be used during the execution
     */
    @Override
	public void up(LogLevel logLevel) {
		this.cli.run(new DockerCliCommand.ComposeUp(logLevel));
	}

	/**
     * Shuts down the Docker Compose environment.
     * 
     * @param timeout the duration to wait for the shutdown to complete
     */
    @Override
	public void down(Duration timeout) {
		this.cli.run(new DockerCliCommand.ComposeDown(timeout));
	}

	/**
     * Starts the Docker Compose CLI with the specified log level.
     * 
     * @param logLevel the log level to use for the Docker Compose CLI
     */
    @Override
	public void start(LogLevel logLevel) {
		this.cli.run(new DockerCliCommand.ComposeStart(logLevel));
	}

	/**
     * Stops the Docker Compose services with the specified timeout.
     *
     * @param timeout the duration to wait for the services to stop
     */
    @Override
	public void stop(Duration timeout) {
		this.cli.run(new DockerCliCommand.ComposeStop(timeout));
	}

	/**
     * Checks if there are any defined services in the Docker Compose configuration.
     * 
     * @return true if there are defined services, false otherwise
     */
    @Override
	public boolean hasDefinedServices() {
		return !this.cli.run(new DockerCliCommand.ComposeConfig()).services().isEmpty();
	}

	/**
     * Retrieves a list of running services.
     * 
     * @return A list of RunningService objects representing the running services.
     */
    @Override
	public List<RunningService> getRunningServices() {
		List<DockerCliComposePsResponse> runningPsResponses = runComposePs().stream().filter(this::isRunning).toList();
		if (runningPsResponses.isEmpty()) {
			return Collections.emptyList();
		}
		DockerComposeFile dockerComposeFile = this.cli.getDockerComposeFile();
		List<RunningService> result = new ArrayList<>();
		Map<String, DockerCliInspectResponse> inspected = inspect(runningPsResponses);
		for (DockerCliComposePsResponse psResponse : runningPsResponses) {
			DockerCliInspectResponse inspectResponse = inspectContainer(psResponse.id(), inspected);
			Assert.notNull(inspectResponse, () -> "Failed to inspect container '%s'".formatted(psResponse.id()));
			result.add(new DefaultRunningService(this.hostname, dockerComposeFile, psResponse, inspectResponse));
		}
		return Collections.unmodifiableList(result);
	}

	/**
     * Inspects the running Docker containers based on the given list of DockerCliComposePsResponse objects.
     * 
     * @param runningPsResponses the list of DockerCliComposePsResponse objects representing the running containers
     * @return a map of container IDs to DockerCliInspectResponse objects containing the inspection results
     */
    private Map<String, DockerCliInspectResponse> inspect(List<DockerCliComposePsResponse> runningPsResponses) {
		List<String> ids = runningPsResponses.stream().map(DockerCliComposePsResponse::id).toList();
		List<DockerCliInspectResponse> inspectResponses = this.cli.run(new DockerCliCommand.Inspect(ids));
		return inspectResponses.stream().collect(Collectors.toMap(DockerCliInspectResponse::id, Function.identity()));
	}

	/**
     * Inspects a Docker container with the given ID.
     * 
     * @param id        the ID of the container to inspect
     * @param inspected a map containing previously inspected containers
     * @return the DockerCliInspectResponse object representing the inspection result, or null if the container was not found
     */
    private DockerCliInspectResponse inspectContainer(String id, Map<String, DockerCliInspectResponse> inspected) {
		DockerCliInspectResponse inspect = inspected.get(id);
		if (inspect != null) {
			return inspect;
		}
		// Docker Compose v2.23.0 returns truncated ids, so we have to do a prefix match
		for (Entry<String, DockerCliInspectResponse> entry : inspected.entrySet()) {
			if (entry.getKey().startsWith(id)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
     * Runs the "docker-compose ps" command and returns the list of DockerCliComposePsResponse objects.
     * 
     * @return the list of DockerCliComposePsResponse objects representing the output of the command
     */
    private List<DockerCliComposePsResponse> runComposePs() {
		return this.cli.run(new DockerCliCommand.ComposePs());
	}

	/**
     * Checks if the Docker container is running.
     * 
     * @param psResponse the response from the Docker CLI compose ps command
     * @return true if the container is running, false otherwise
     */
    private boolean isRunning(DockerCliComposePsResponse psResponse) {
		return !"exited".equals(psResponse.state());
	}

}
