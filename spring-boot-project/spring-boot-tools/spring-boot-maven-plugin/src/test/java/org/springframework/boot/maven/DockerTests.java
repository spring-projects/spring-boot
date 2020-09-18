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
		assertThat(docker.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
		assertThat(docker.asDockerConfiguration().getPublishRegistryAuthentication()).isNull();
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
		assertThat(docker.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
		assertThat(docker.asDockerConfiguration().getPublishRegistryAuthentication()).isNull();
	}

	@Test
	void asDockerConfigurationWithUserAuth() {
		Docker docker = new Docker();
		docker.setBuilderRegistry(
				new Docker.DockerRegistry("user1", "secret1", "https://docker1.example.com", "docker1@example.com"));
		docker.setPublishRegistry(
				new Docker.DockerRegistry("user2", "secret2", "https://docker2.example.com", "docker2@example.com"));
		DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
		assertThat(decoded(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()))
				.contains("\"username\" : \"user1\"").contains("\"password\" : \"secret1\"")
				.contains("\"email\" : \"docker1@example.com\"")
				.contains("\"serveraddress\" : \"https://docker1.example.com\"");
		assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
				.contains("\"username\" : \"user2\"").contains("\"password\" : \"secret2\"")
				.contains("\"email\" : \"docker2@example.com\"")
				.contains("\"serveraddress\" : \"https://docker2.example.com\"");
	}

	@Test
	void asDockerConfigurationWithIncompleteBuilderUserAuthFails() {
		Docker docker = new Docker();
		docker.setBuilderRegistry(
				new Docker.DockerRegistry("user", null, "https://docker.example.com", "docker@example.com"));
		assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
				.withMessageContaining("Invalid Docker builder registry configuration");
	}

	@Test
	void asDockerConfigurationWithIncompletePublishUserAuthFails() {
		Docker docker = new Docker();
		docker.setPublishRegistry(
				new Docker.DockerRegistry("user", null, "https://docker.example.com", "docker@example.com"));
		assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
				.withMessageContaining("Invalid Docker publish registry configuration");
	}

	@Test
	void asDockerConfigurationWithTokenAuth() {
		Docker docker = new Docker();
		docker.setBuilderRegistry(new Docker.DockerRegistry("token1"));
		docker.setPublishRegistry(new Docker.DockerRegistry("token2"));
		DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
		assertThat(decoded(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()))
				.contains("\"identitytoken\" : \"token1\"");
		assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
				.contains("\"identitytoken\" : \"token2\"");
	}

	@Test
	void asDockerConfigurationWithUserAndTokenAuthFails() {
		Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
		dockerRegistry.setUsername("user");
		dockerRegistry.setPassword("secret");
		dockerRegistry.setToken("token");
		Docker docker = new Docker();
		docker.setBuilderRegistry(dockerRegistry);
		assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
				.withMessageContaining("Invalid Docker builder registry configuration");
	}

	String decoded(String value) {
		return new String(Base64Utils.decodeFromString(value));
	}

}
