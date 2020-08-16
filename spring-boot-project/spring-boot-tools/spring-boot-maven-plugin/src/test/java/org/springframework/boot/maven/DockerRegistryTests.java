/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.maven;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerRegistry}.
 *
 * @author Wei Jiang
 */
public class DockerRegistryTests {

	@Test
	void getDockerRegistryConfiguration() {
		DockerRegistry dockerRegistry = new DockerRegistry();
		dockerRegistry.setUsername("username");
		dockerRegistry.setPassword("password");
		dockerRegistry.setEmail("mock@spring.com");
		dockerRegistry.setUrl("http://mock.docker.registry");
		DockerRegistryConfiguration dockerRegistryConfiguration = dockerRegistry.getDockerRegistryConfiguration();
		assertThat(dockerRegistryConfiguration).isNotNull();
		assertThat(dockerRegistryConfiguration.getUsername()).isEqualTo(dockerRegistry.getUsername());
		assertThat(dockerRegistryConfiguration.getPassword()).isEqualTo(dockerRegistry.getPassword());
		assertThat(dockerRegistryConfiguration.getEmail()).isEqualTo(dockerRegistry.getEmail());
		assertThat(dockerRegistryConfiguration.getUrl()).isEqualTo(dockerRegistry.getUrl());
	}

}
