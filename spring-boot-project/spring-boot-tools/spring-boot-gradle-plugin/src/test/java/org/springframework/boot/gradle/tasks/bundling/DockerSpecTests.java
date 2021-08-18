/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.gradle.tasks.bundling;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerHost;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DockerSpec}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
class DockerSpecTests {

	@Test
	void asDockerConfigurationWithDefaults() {
		DockerSpec dockerSpec = new DockerSpec();
		assertThat(dockerSpec.asDockerConfiguration().getHost()).isNull();
		assertThat(dockerSpec.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
		assertThat(dockerSpec.asDockerConfiguration().getPublishRegistryAuthentication()).isNull();
	}

	@Test
	void asDockerConfigurationWithHostConfiguration() {
		DockerSpec dockerSpec = new DockerSpec();
		dockerSpec.setHost("docker.example.com");
		dockerSpec.setTlsVerify(true);
		dockerSpec.setCertPath("/tmp/ca-cert");
		DockerConfiguration dockerConfiguration = dockerSpec.asDockerConfiguration();
		DockerHost host = dockerConfiguration.getHost();
		assertThat(host.getAddress()).isEqualTo("docker.example.com");
		assertThat(host.isSecure()).isEqualTo(true);
		assertThat(host.getCertificatePath()).isEqualTo("/tmp/ca-cert");
		assertThat(dockerSpec.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
		assertThat(dockerSpec.asDockerConfiguration().getPublishRegistryAuthentication()).isNull();
	}

	@Test
	void asDockerConfigurationWithHostConfigurationNoTlsVerify() {
		DockerSpec dockerSpec = new DockerSpec();
		dockerSpec.setHost("docker.example.com");
		DockerConfiguration dockerConfiguration = dockerSpec.asDockerConfiguration();
		DockerHost host = dockerConfiguration.getHost();
		assertThat(host.getAddress()).isEqualTo("docker.example.com");
		assertThat(host.isSecure()).isEqualTo(false);
		assertThat(host.getCertificatePath()).isNull();
		assertThat(dockerSpec.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
		assertThat(dockerSpec.asDockerConfiguration().getPublishRegistryAuthentication()).isNull();
	}

	@Test
	void asDockerConfigurationWithUserAuth() {
		DockerSpec dockerSpec = new DockerSpec(
				new DockerSpec.DockerRegistrySpec("user1", "secret1", "https://docker1.example.com",
						"docker1@example.com"),
				new DockerSpec.DockerRegistrySpec("user2", "secret2", "https://docker2.example.com",
						"docker2@example.com"));
		DockerConfiguration dockerConfiguration = dockerSpec.asDockerConfiguration();
		assertThat(decoded(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()))
				.contains("\"username\" : \"user1\"").contains("\"password\" : \"secret1\"")
				.contains("\"email\" : \"docker1@example.com\"")
				.contains("\"serveraddress\" : \"https://docker1.example.com\"");
		assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
				.contains("\"username\" : \"user2\"").contains("\"password\" : \"secret2\"")
				.contains("\"email\" : \"docker2@example.com\"")
				.contains("\"serveraddress\" : \"https://docker2.example.com\"");
		assertThat(dockerSpec.asDockerConfiguration().getHost()).isNull();
	}

	@Test
	void asDockerConfigurationWithIncompleteBuilderUserAuthFails() {
		DockerSpec.DockerRegistrySpec builderRegistry = new DockerSpec.DockerRegistrySpec("user", null,
				"https://docker.example.com", "docker@example.com");
		DockerSpec dockerSpec = new DockerSpec(builderRegistry, null);
		assertThatExceptionOfType(GradleException.class).isThrownBy(dockerSpec::asDockerConfiguration)
				.withMessageContaining("Invalid Docker builder registry configuration");
	}

	@Test
	void asDockerConfigurationWithIncompletePublishUserAuthFails() {
		DockerSpec.DockerRegistrySpec publishRegistry = new DockerSpec.DockerRegistrySpec("user2", null,
				"https://docker2.example.com", "docker2@example.com");
		DockerSpec dockerSpec = new DockerSpec(null, publishRegistry);
		assertThatExceptionOfType(GradleException.class).isThrownBy(dockerSpec::asDockerConfiguration)
				.withMessageContaining("Invalid Docker publish registry configuration");
	}

	@Test
	void asDockerConfigurationWithTokenAuth() {
		DockerSpec dockerSpec = new DockerSpec(new DockerSpec.DockerRegistrySpec("token1"),
				new DockerSpec.DockerRegistrySpec("token2"));
		DockerConfiguration dockerConfiguration = dockerSpec.asDockerConfiguration();
		assertThat(decoded(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()))
				.contains("\"identitytoken\" : \"token1\"");
		assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
				.contains("\"identitytoken\" : \"token2\"");
	}

	@Test
	void asDockerConfigurationWithUserAndTokenAuthFails() {
		DockerSpec.DockerRegistrySpec builderRegistry = new DockerSpec.DockerRegistrySpec();
		builderRegistry.setUsername("user");
		builderRegistry.setPassword("secret");
		builderRegistry.setToken("token");
		DockerSpec dockerSpec = new DockerSpec(builderRegistry, null);
		assertThatExceptionOfType(GradleException.class).isThrownBy(dockerSpec::asDockerConfiguration)
				.withMessageContaining("Invalid Docker builder registry configuration");
	}

	String decoded(String value) {
		return new String(Base64Utils.decodeFromString(value));
	}

}
