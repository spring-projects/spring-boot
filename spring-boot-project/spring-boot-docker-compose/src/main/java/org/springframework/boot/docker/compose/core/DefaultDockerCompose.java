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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.logging.LogLevel;

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

	DefaultDockerCompose(DockerCli cli, String host) {
		this.cli = cli;
		this.hostname = DockerHost.get(host, () -> cli.run(new DockerCliCommand.Context()));
	}

	@Override
	public void up(LogLevel logLevel) {
		this.cli.run(new DockerCliCommand.ComposeUp(logLevel));
	}

	@Override
	public void down(Duration timeout) {
		this.cli.run(new DockerCliCommand.ComposeDown(timeout));
	}

	@Override
	public void start(LogLevel logLevel) {
		this.cli.run(new DockerCliCommand.ComposeStart(logLevel));
	}

	@Override
	public void stop(Duration timeout) {
		this.cli.run(new DockerCliCommand.ComposeStop(timeout));
	}

	@Override
	public boolean hasDefinedServices() {
		return !this.cli.run(new DockerCliCommand.ComposeConfig()).services().isEmpty();
	}

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
			DockerCliInspectResponse inspectResponse = inspected.get(psResponse.id());
			result.add(new DefaultRunningService(this.hostname, dockerComposeFile, psResponse, inspectResponse));
		}
		return Collections.unmodifiableList(result);
	}

	private Map<String, DockerCliInspectResponse> inspect(List<DockerCliComposePsResponse> runningPsResponses) {
		List<String> ids = runningPsResponses.stream().map(DockerCliComposePsResponse::id).toList();
		List<DockerCliInspectResponse> inspectResponses = this.cli.run(new DockerCliCommand.Inspect(ids));
		return inspectResponses.stream().collect(Collectors.toMap(DockerCliInspectResponse::id, Function.identity()));
	}

	private List<DockerCliComposePsResponse> runComposePs() {
		return this.cli.run(new DockerCliCommand.ComposePs());
	}

	private boolean isRunning(DockerCliComposePsResponse psResponse) {
		return !"exited".equals(psResponse.state());
	}

}
