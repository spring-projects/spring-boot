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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.Config;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.ExposedPort;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.HostConfig;
import org.springframework.boot.docker.compose.core.DockerCliInspectResponse.NetworkSettings;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultDockerCompose}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultDockerComposeTests {

	private static final String HOST = "192.168.1.1";

	private DockerCli cli = mock(DockerCli.class);

	@Test
	void upRunsUpCommand() {
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, HOST);
		compose.up(LogLevel.OFF);
		then(this.cli).should().run(new DockerCliCommand.ComposeUp(LogLevel.OFF));
	}

	@Test
	void downRunsDownCommand() {
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, HOST);
		Duration timeout = Duration.ofSeconds(1);
		compose.down(timeout);
		then(this.cli).should().run(new DockerCliCommand.ComposeDown(timeout));
	}

	@Test
	void startRunsStartCommand() {
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, HOST);
		compose.start(LogLevel.OFF);
		then(this.cli).should().run(new DockerCliCommand.ComposeStart(LogLevel.OFF));
	}

	@Test
	void stopRunsStopCommand() {
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, HOST);
		Duration timeout = Duration.ofSeconds(1);
		compose.stop(timeout);
		then(this.cli).should().run(new DockerCliCommand.ComposeStop(timeout));
	}

	@Test
	void hasDefinedServicesWhenComposeConfigServicesIsEmptyReturnsFalse() {
		willReturn(new DockerCliComposeConfigResponse("test", Collections.emptyMap())).given(this.cli)
			.run(new DockerCliCommand.ComposeConfig());
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, HOST);
		assertThat(compose.hasDefinedServices()).isFalse();
	}

	@Test
	void hasDefinedServicesWhenComposeConfigServicesIsNotEmptyReturnsTrue() {
		willReturn(new DockerCliComposeConfigResponse("test",
				Map.of("redis", new DockerCliComposeConfigResponse.Service("redis"))))
			.given(this.cli)
			.run(new DockerCliCommand.ComposeConfig());
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, HOST);
		assertThat(compose.hasDefinedServices()).isTrue();
	}

	@Test
	void getRunningServicesReturnsServices() {
		String id = "123";
		DockerCliComposePsResponse psResponse = new DockerCliComposePsResponse(id, "name", "redis", "running");
		Map<String, ExposedPort> exposedPorts = Collections.emptyMap();
		Config config = new Config("redis", Map.of("spring", "boot"), exposedPorts, List.of("a=b"));
		NetworkSettings networkSettings = null;
		HostConfig hostConfig = null;
		DockerCliInspectResponse inspectResponse = new DockerCliInspectResponse(id, config, networkSettings,
				hostConfig);
		willReturn(List.of(psResponse)).given(this.cli).run(new DockerCliCommand.ComposePs());
		willReturn(List.of(inspectResponse)).given(this.cli).run(new DockerCliCommand.Inspect(List.of(id)));
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, HOST);
		List<RunningService> runningServices = compose.getRunningServices();
		assertThat(runningServices).hasSize(1);
		RunningService runningService = runningServices.get(0);
		assertThat(runningService.name()).isEqualTo("name");
		assertThat(runningService.image()).hasToString("docker.io/library/redis");
		assertThat(runningService.host()).isEqualTo(HOST);
		assertThat(runningService.ports().getAll()).isEmpty();
		assertThat(runningService.env()).containsExactly(entry("a", "b"));
		assertThat(runningService.labels()).containsExactly(entry("spring", "boot"));
	}

	@Test
	void getRunningServicesWhenNoHostUsesHostFromContext() {
		String id = "123";
		DockerCliComposePsResponse psResponse = new DockerCliComposePsResponse(id, "name", "redis", "running");
		Map<String, ExposedPort> exposedPorts = Collections.emptyMap();
		Config config = new Config("redis", Map.of("spring", "boot"), exposedPorts, List.of("a=b"));
		NetworkSettings networkSettings = null;
		HostConfig hostConfig = null;
		DockerCliInspectResponse inspectResponse = new DockerCliInspectResponse(id, config, networkSettings,
				hostConfig);
		willReturn(List.of(new DockerCliContextResponse("test", true, "https://192.168.1.1"))).given(this.cli)
			.run(new DockerCliCommand.Context());
		willReturn(List.of(psResponse)).given(this.cli).run(new DockerCliCommand.ComposePs());
		willReturn(List.of(inspectResponse)).given(this.cli).run(new DockerCliCommand.Inspect(List.of(id)));
		DefaultDockerCompose compose = new DefaultDockerCompose(this.cli, null);
		List<RunningService> runningServices = compose.getRunningServices();
		assertThat(runningServices).hasSize(1);
		RunningService runningService = runningServices.get(0);
		assertThat(runningService.host()).isEqualTo("192.168.1.1");
	}

}
