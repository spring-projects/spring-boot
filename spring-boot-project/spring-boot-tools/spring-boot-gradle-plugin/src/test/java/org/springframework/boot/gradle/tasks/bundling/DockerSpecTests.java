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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.Base64;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.build.BuilderDockerConfiguration;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConnectionConfiguration;
import org.springframework.boot.gradle.junit.GradleProjectBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DockerSpec}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
class DockerSpecTests {

	private DockerSpec dockerSpec;

	@BeforeEach
	void prepareDockerSpec(@TempDir File temp) {
		this.dockerSpec = GradleProjectBuilder.builder()
			.withProjectDir(temp)
			.build()
			.getObjects()
			.newInstance(DockerSpec.class);
	}

	@Test
	void asDockerConfigurationWithDefaults() {
		BuilderDockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
		assertThat(dockerConfiguration.connection()).isNull();
		assertThat(dockerConfiguration.builderRegistryAuthentication().getAuthHeader()).isNull();
		assertThat(decoded(dockerConfiguration.publishRegistryAuthentication().getAuthHeader()))
			.contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithHostConfiguration() {
		this.dockerSpec.getHost().set("docker.example.com");
		this.dockerSpec.getTlsVerify().set(true);
		this.dockerSpec.getCertPath().set("/tmp/ca-cert");
		BuilderDockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
		DockerConnectionConfiguration.Host host = (DockerConnectionConfiguration.Host) dockerConfiguration.connection();
		assertThat(host.address()).isEqualTo("docker.example.com");
		assertThat(host.secure()).isTrue();
		assertThat(host.certificatePath()).isEqualTo("/tmp/ca-cert");
		assertThat(dockerConfiguration.bindHostToBuilder()).isFalse();
		assertThat(dockerConfiguration.builderRegistryAuthentication().getAuthHeader()).isNull();
		assertThat(decoded(dockerConfiguration.publishRegistryAuthentication().getAuthHeader()))
			.contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithHostConfigurationNoTlsVerify() {
		this.dockerSpec.getHost().set("docker.example.com");
		BuilderDockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
		DockerConnectionConfiguration.Host host = (DockerConnectionConfiguration.Host) dockerConfiguration.connection();
		assertThat(host.address()).isEqualTo("docker.example.com");
		assertThat(host.secure()).isFalse();
		assertThat(host.certificatePath()).isNull();
		assertThat(dockerConfiguration.bindHostToBuilder()).isFalse();
		assertThat(dockerConfiguration.builderRegistryAuthentication().getAuthHeader()).isNull();
		assertThat(decoded(dockerConfiguration.publishRegistryAuthentication().getAuthHeader()))
			.contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithContextConfiguration() {
		this.dockerSpec.getContext().set("test-context");
		BuilderDockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
		DockerConnectionConfiguration.Context host = (DockerConnectionConfiguration.Context) dockerConfiguration
			.connection();
		assertThat(host.context()).isEqualTo("test-context");
		assertThat(dockerConfiguration.bindHostToBuilder()).isFalse();
		assertThat(dockerConfiguration.builderRegistryAuthentication().getAuthHeader()).isNull();
		assertThat(decoded(dockerConfiguration.publishRegistryAuthentication().getAuthHeader()))
			.contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithHostAndContextFails() {
		this.dockerSpec.getContext().set("test-context");
		this.dockerSpec.getHost().set("docker.example.com");
		assertThatExceptionOfType(GradleException.class).isThrownBy(this.dockerSpec::asDockerConfiguration)
			.withMessageContaining("Invalid Docker configuration");
	}

	@Test
	void asDockerConfigurationWithBindHostToBuilder() {
		this.dockerSpec.getHost().set("docker.example.com");
		this.dockerSpec.getBindHostToBuilder().set(true);
		BuilderDockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
		DockerConnectionConfiguration.Host host = (DockerConnectionConfiguration.Host) dockerConfiguration.connection();
		assertThat(host.address()).isEqualTo("docker.example.com");
		assertThat(host.secure()).isFalse();
		assertThat(host.certificatePath()).isNull();
		assertThat(dockerConfiguration.bindHostToBuilder()).isTrue();
		assertThat(dockerConfiguration.builderRegistryAuthentication().getAuthHeader()).isNull();
		assertThat(decoded(dockerConfiguration.publishRegistryAuthentication().getAuthHeader()))
			.contains("\"username\" : \"\"")
			.contains("\"password\" : \"\"")
			.contains("\"email\" : \"\"")
			.contains("\"serveraddress\" : \"\"");
	}

	@Test
	void asDockerConfigurationWithUserAuth() {
		this.dockerSpec.builderRegistry((registry) -> {
			registry.getUsername().set("user1");
			registry.getPassword().set("secret1");
			registry.getUrl().set("https://docker1.example.com");
			registry.getEmail().set("docker1@example.com");
		});
		this.dockerSpec.publishRegistry((registry) -> {
			registry.getUsername().set("user2");
			registry.getPassword().set("secret2");
			registry.getUrl().set("https://docker2.example.com");
			registry.getEmail().set("docker2@example.com");
		});
		BuilderDockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
		assertThat(decoded(dockerConfiguration.builderRegistryAuthentication().getAuthHeader()))
			.contains("\"username\" : \"user1\"")
			.contains("\"password\" : \"secret1\"")
			.contains("\"email\" : \"docker1@example.com\"")
			.contains("\"serveraddress\" : \"https://docker1.example.com\"");
		assertThat(decoded(dockerConfiguration.publishRegistryAuthentication().getAuthHeader()))
			.contains("\"username\" : \"user2\"")
			.contains("\"password\" : \"secret2\"")
			.contains("\"email\" : \"docker2@example.com\"")
			.contains("\"serveraddress\" : \"https://docker2.example.com\"");
		assertThat(dockerConfiguration.connection()).isNull();
	}

	@Test
	void asDockerConfigurationWithIncompleteBuilderUserAuthFails() {
		this.dockerSpec.builderRegistry((registry) -> {
			registry.getUsername().set("user1");
			registry.getUrl().set("https://docker1.example.com");
			registry.getEmail().set("docker1@example.com");
		});
		assertThatExceptionOfType(GradleException.class).isThrownBy(this.dockerSpec::asDockerConfiguration)
			.withMessageContaining("Invalid Docker builder registry configuration");
	}

	@Test
	void asDockerConfigurationWithIncompletePublishUserAuthFails() {
		this.dockerSpec.publishRegistry((registry) -> {
			registry.getUsername().set("user2");
			registry.getUrl().set("https://docker2.example.com");
			registry.getEmail().set("docker2@example.com");
		});
		assertThatExceptionOfType(GradleException.class).isThrownBy(this.dockerSpec::asDockerConfiguration)
			.withMessageContaining("Invalid Docker publish registry configuration");
	}

	@Test
	void asDockerConfigurationWithTokenAuth() {
		this.dockerSpec.builderRegistry((registry) -> registry.getToken().set("token1"));
		this.dockerSpec.publishRegistry((registry) -> registry.getToken().set("token2"));
		BuilderDockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
		assertThat(decoded(dockerConfiguration.builderRegistryAuthentication().getAuthHeader()))
			.contains("\"identitytoken\" : \"token1\"");
		assertThat(decoded(dockerConfiguration.publishRegistryAuthentication().getAuthHeader()))
			.contains("\"identitytoken\" : \"token2\"");
	}

	@Test
	void asDockerConfigurationWithUserAndTokenAuthFails() {
		this.dockerSpec.builderRegistry((registry) -> {
			registry.getUsername().set("user");
			registry.getPassword().set("secret");
			registry.getToken().set("token");
		});
		assertThatExceptionOfType(GradleException.class).isThrownBy(this.dockerSpec::asDockerConfiguration)
			.withMessageContaining("Invalid Docker builder registry configuration");
	}

	String decoded(String value) {
		return (value != null) ? new String(Base64.getDecoder().decode(value)) : value;
	}

}
