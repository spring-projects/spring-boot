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

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerHost;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerRegistryAuthentication;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Docker}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
public class DockerTests {

	@Test
	void asDockerConfigurationWithDefaults() {
		Docker docker = new Docker();
		assertThat(docker.asDockerConfiguration().getHost()).isNull();
		assertThat(docker.asDockerConfiguration().getRegistryAuthentication()).isNull();
	}

	@Test
	void asDockerConfigurationWithHostConfiguration() {
		Docker docker = new Docker();
		docker.setHost("docker.example.com");
		docker.setTlsVerify(true);
		docker.setCertPath("/tmp/ca-cert");
		DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
		DockerHost host = dockerConfiguration.getHost();
		assertThat(host.getAddress()).isEqualTo("docker.example.com");
		assertThat(host.isSecure()).isEqualTo(true);
		assertThat(host.getCertificatePath()).isEqualTo("/tmp/ca-cert");
		assertThat(docker.asDockerConfiguration().getRegistryAuthentication()).isNull();
	}

	@Test
	void asDockerConfigurationWithUserAuth() {
		Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
		dockerRegistry.setUsername("user");
		dockerRegistry.setPassword("secret");
		dockerRegistry.setUrl("https://docker.example.com");
		dockerRegistry.setEmail("docker@example.com");
		Docker docker = new Docker();
		docker.setRegistry(dockerRegistry);
		DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
		DockerRegistryAuthentication registryAuthentication = dockerConfiguration.getRegistryAuthentication();
		assertThat(registryAuthentication).isNotNull();
		assertThat(new String(Base64Utils.decodeFromString(registryAuthentication.createAuthHeader())))
				.contains("\"username\" : \"user\"").contains("\"password\" : \"secret\"")
				.contains("\"email\" : \"docker@example.com\"")
				.contains("\"serveraddress\" : \"https://docker.example.com\"");
	}

	@Test
	void asDockerConfigurationWithIncompleteUserAuthFails() {
		Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
		dockerRegistry.setUsername("user");
		dockerRegistry.setUrl("https://docker.example.com");
		dockerRegistry.setEmail("docker@example.com");
		Docker docker = new Docker();
		docker.setRegistry(dockerRegistry);
		assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
				.withMessageContaining("Invalid Docker registry configuration");
	}

	@Test
	void asDockerConfigurationWithTokenAuth() {
		Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
		dockerRegistry.setToken("token");
		Docker docker = new Docker();
		docker.setRegistry(dockerRegistry);
		DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
		DockerRegistryAuthentication registryAuthentication = dockerConfiguration.getRegistryAuthentication();
		assertThat(registryAuthentication).isNotNull();
		assertThat(new String(Base64Utils.decodeFromString(registryAuthentication.createAuthHeader())))
				.contains("\"identitytoken\" : \"token\"");
	}

	@Test
	void asDockerConfigurationWithUserAndTokenAuthFails() {
		Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
		dockerRegistry.setUsername("user");
		dockerRegistry.setPassword("secret");
		dockerRegistry.setToken("token");
		Docker docker = new Docker();
		docker.setRegistry(dockerRegistry);
		assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
				.withMessageContaining("Invalid Docker registry configuration");
	}

}
