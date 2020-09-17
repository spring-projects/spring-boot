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

package org.springframework.boot.buildpack.platform.docker.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerConfiguration}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
public class DockerConfigurationTests {

	@Test
	void createDockerConfigurationWithDefaults() {
		DockerConfiguration configuration = new DockerConfiguration();
		assertThat(configuration.getRegistryAuthentication()).isNull();
	}

	@Test
	void createDockerConfigurationWithUserAuth() {
		DockerConfiguration configuration = new DockerConfiguration().withRegistryUserAuthentication("user", "secret",
				"https://docker.example.com", "docker@example.com");
		DockerRegistryAuthentication auth = configuration.getRegistryAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth).isInstanceOf(DockerRegistryUserAuthentication.class);
		DockerRegistryUserAuthentication userAuth = (DockerRegistryUserAuthentication) auth;
		assertThat(userAuth.getUrl()).isEqualTo("https://docker.example.com");
		assertThat(userAuth.getUsername()).isEqualTo("user");
		assertThat(userAuth.getPassword()).isEqualTo("secret");
		assertThat(userAuth.getEmail()).isEqualTo("docker@example.com");
	}

	@Test
	void createDockerConfigurationWithTokenAuth() {
		DockerConfiguration configuration = new DockerConfiguration().withRegistryTokenAuthentication("token");
		DockerRegistryAuthentication auth = configuration.getRegistryAuthentication();
		assertThat(auth).isNotNull();
		assertThat(auth).isInstanceOf(DockerRegistryTokenAuthentication.class);
		DockerRegistryTokenAuthentication tokenAuth = (DockerRegistryTokenAuthentication) auth;
		assertThat(tokenAuth.getToken()).isEqualTo("token");
	}

}
