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

package org.springframework.boot.buildpack.platform.docker.configuration;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.DockerConfig;
import org.springframework.boot.buildpack.platform.docker.configuration.DockerConfigurationMetadata.DockerContext;
import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerConfigurationMetadata}.
 *
 * @author Scott Frederick
 * @author Dmytro Nosan
 */
class DockerConfigurationMetadataTests extends AbstractJsonTests {

	private final Map<String, String> environment = new LinkedHashMap<>();

	@Test
	void configWithContextIsRead() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-context/config.json"));
		DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
		assertThat(config.getConfiguration().getCurrentContext()).isEqualTo("test-context");
		assertThat(config.getConfiguration().getAuths()).isEmpty();
		assertThat(config.getConfiguration().getCredHelpers()).isEmpty();
		assertThat(config.getConfiguration().getCredsStore()).isNull();
		assertThat(config.getContext().getDockerHost()).isEqualTo("unix:///home/user/.docker/docker.sock");
		assertThat(config.getContext().isTlsVerify()).isFalse();
		assertThat(config.getContext().getTlsPath()).isNull();
	}

	@Test
	void configWithoutContextIsRead() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("without-context/config.json"));
		DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
		assertThat(config.getConfiguration().getCurrentContext()).isNull();
		assertThat(config.getConfiguration().getAuths()).isEmpty();
		assertThat(config.getConfiguration().getCredHelpers()).isEmpty();
		assertThat(config.getConfiguration().getCredsStore()).isNull();
		assertThat(config.getContext().getDockerHost()).isNull();
		assertThat(config.getContext().isTlsVerify()).isFalse();
		assertThat(config.getContext().getTlsPath()).isNull();
	}

	@Test
	void configWithDefaultContextIsRead() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
		DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
		assertThat(config.getConfiguration().getCurrentContext()).isEqualTo("default");
		assertThat(config.getConfiguration().getAuths()).isEmpty();
		assertThat(config.getConfiguration().getCredHelpers()).isEmpty();
		assertThat(config.getConfiguration().getCredsStore()).isNull();
		assertThat(config.getContext().getDockerHost()).isNull();
		assertThat(config.getContext().isTlsVerify()).isFalse();
		assertThat(config.getContext().getTlsPath()).isNull();
	}

	@Test
	void configIsReadWithProvidedContext() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
		DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
		DockerContext context = config.forContext("test-context");
		assertThat(context.getDockerHost()).isEqualTo("unix:///home/user/.docker/docker.sock");
		assertThat(context.isTlsVerify()).isTrue();
		assertThat(context.getTlsPath()).matches(String.join(Pattern.quote(File.separator), "^.*",
				"with-default-context", "contexts", "tls", "[a-zA-z0-9]*", "docker$"));
	}

	@Test
	void invalidContextThrowsException() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
		assertThatIllegalArgumentException()
			.isThrownBy(() -> DockerConfigurationMetadata.from(this.environment::get).forContext("invalid-context"))
			.withMessageContaining("Docker context 'invalid-context' does not exist");
	}

	@Test
	void configIsEmptyWhenConfigFileDoesNotExist() {
		this.environment.put("DOCKER_CONFIG", "docker-config-dummy-path");
		DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
		assertThat(config.getConfiguration().getCurrentContext()).isNull();
		assertThat(config.getConfiguration().getAuths()).isEmpty();
		assertThat(config.getConfiguration().getCredHelpers()).isEmpty();
		assertThat(config.getConfiguration().getCredsStore()).isNull();
		assertThat(config.getContext().getDockerHost()).isNull();
		assertThat(config.getContext().isTlsVerify()).isFalse();
	}

	@Test
	void configWithAuthIsRead() throws Exception {
		this.environment.put("DOCKER_CONFIG", pathToResource("with-auth/config.json"));
		DockerConfigurationMetadata metadata = DockerConfigurationMetadata.from(this.environment::get);
		DockerConfig configuration = metadata.getConfiguration();
		assertThat(configuration.getCredsStore()).isEqualTo("desktop");
		assertThat(configuration.getCredHelpers()).hasSize(3)
			.containsEntry("azurecr.io", "acr-env")
			.containsEntry("ecr.us-east-1.amazonaws.com", "ecr-login")
			.containsEntry("gcr.io", "gcr");
		assertThat(configuration.getAuths()).hasSize(3).hasEntrySatisfying("https://index.docker.io/v1/", (auth) -> {
			assertThat(auth.getUsername()).isEqualTo("username");
			assertThat(auth.getPassword()).isEqualTo("pass\u0000word");
			assertThat(auth.getEmail()).isEqualTo("test@example.com");
		}).hasEntrySatisfying("custom-registry.example.com", (auth) -> {
			assertThat(auth.getUsername()).isEqualTo("customUser");
			assertThat(auth.getPassword()).isEqualTo("customPass");
			assertThat(auth.getEmail()).isNull();
		}).hasEntrySatisfying("my-registry.example.com", (auth) -> {
			assertThat(auth.getUsername()).isEqualTo("user");
			assertThat(auth.getPassword()).isEqualTo("password");
			assertThat(auth.getEmail()).isNull();
		});
	}

	private String pathToResource(String resource) throws URISyntaxException {
		URL url = getClass().getResource(resource);
		return Paths.get(url.toURI()).getParent().toAbsolutePath().toString();
	}

}
