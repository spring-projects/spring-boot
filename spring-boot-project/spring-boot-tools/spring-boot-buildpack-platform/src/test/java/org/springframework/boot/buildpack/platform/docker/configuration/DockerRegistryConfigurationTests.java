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

import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerRegistryConfiguration}.
 *
 * @author Wei Jiang
 */
public class DockerRegistryConfigurationTests {

	@Test
	void createDockerRegistryAuthTokenWithToken() {
		DockerRegistryConfiguration dockerRegistryConfiguration = new DockerRegistryConfiguration();
		dockerRegistryConfiguration.setToken("mockToken");
		assertThat(dockerRegistryConfiguration.createDockerRegistryAuthToken()).isEqualTo("mockToken");
	}

	@Test
	void createDockerRegistryAuthTokenWithoutToken() {
		DockerRegistryConfiguration dockerRegistryConfiguration = new DockerRegistryConfiguration();
		dockerRegistryConfiguration.setUsername("username");
		dockerRegistryConfiguration.setPassword("password");
		dockerRegistryConfiguration.setEmail("mock@spring.com");
		dockerRegistryConfiguration.setUrl("http://mock.docker.registry");
		String token = dockerRegistryConfiguration.createDockerRegistryAuthToken();
		assertThat(token).isEqualTo(
				"ewogICJ1c2VybmFtZSIgOiAidXNlcm5hbWUiLAogICJwYXNzd29yZCIgOiAicGFzc3dvcmQiLAogICJlbWFpbCIgOiAibW9ja0BzcHJpbmcuY29tIiwKICAic2VydmVyYWRkcmVzcyIgOiAiaHR0cDovL21vY2suZG9ja2VyLnJlZ2lzdHJ5Igp9");
		assertThat(new String(Base64Utils.decodeFromString(token))).isEqualTo("{\n" + "  \"username\" : \"username\",\n"
				+ "  \"password\" : \"password\",\n" + "  \"email\" : \"mock@spring.com\",\n"
				+ "  \"serveraddress\" : \"http://mock.docker.registry\"\n" + "}");
	}

	@Test
	void createDockerRegistryAuthTokenWithUsernameAndPassword() {
		DockerRegistryConfiguration dockerRegistryConfiguration = new DockerRegistryConfiguration();
		dockerRegistryConfiguration.setUsername("username");
		dockerRegistryConfiguration.setPassword("password");
		String token = dockerRegistryConfiguration.createDockerRegistryAuthToken();
		assertThat(dockerRegistryConfiguration.getEmail()).isNull();
		assertThat(dockerRegistryConfiguration.getUrl()).isNull();
		assertThat(token).isEqualTo(
				"ewogICJ1c2VybmFtZSIgOiAidXNlcm5hbWUiLAogICJwYXNzd29yZCIgOiAicGFzc3dvcmQiLAogICJlbWFpbCIgOiBudWxsLAogICJzZXJ2ZXJhZGRyZXNzIiA6IG51bGwKfQ==");
		assertThat(new String(Base64Utils.decodeFromString(token))).isEqualTo("{\n" + "  \"username\" : \"username\",\n"
				+ "  \"password\" : \"password\",\n" + "  \"email\" : null,\n" + "  \"serveraddress\" : null\n" + "}");
	}

	@Test
	void createDockerRegistryAuthTokenWithTokenAndUsername() {
		DockerRegistryConfiguration dockerRegistryConfiguration = new DockerRegistryConfiguration();
		dockerRegistryConfiguration.setToken("mockToken");
		dockerRegistryConfiguration.setUsername("username");
		dockerRegistryConfiguration.setPassword("password");
		dockerRegistryConfiguration.setEmail("mock@spring.com");
		dockerRegistryConfiguration.setUrl("http://mock.docker.registry");
		assertThat(dockerRegistryConfiguration.createDockerRegistryAuthToken()).isEqualTo("mockToken");
	}

}
