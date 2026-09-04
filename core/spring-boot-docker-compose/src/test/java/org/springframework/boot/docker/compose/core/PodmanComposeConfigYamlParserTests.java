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

package org.springframework.boot.docker.compose.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PodmanComposeConfigYamlParser}.
 */
class PodmanComposeConfigYamlParserTests {

	@Test
	void parsesRealisticYaml() {
		String yaml = """
				name: test-project
				services:
				  app:
				    image: my-app:latest
				  db:
				    image: "postgres:15"
				  cache:
				    image: 'redis:alpine'
				    environment:
				      - REDIS_PASSWORD=secret
				""";
		DockerCliComposeConfigResponse response = PodmanComposeConfigYamlParser.parse(yaml);
		assertThat(response.name()).isEqualTo("test-project");
		assertThat(response.services()).hasSize(3);

		DockerCliComposeConfigResponse.Service app = response.services().get("app");
		assertThat(app).isNotNull();
		assertThat(app.image()).isEqualTo("my-app:latest");

		DockerCliComposeConfigResponse.Service db = response.services().get("db");
		assertThat(db).isNotNull();
		assertThat(db.image()).isEqualTo("postgres:15");

		DockerCliComposeConfigResponse.Service cache = response.services().get("cache");
		assertThat(cache).isNotNull();
		assertThat(cache.image()).isEqualTo("redis:alpine");
	}

	@Test
	void parsesServiceWithoutImage() {
		String yaml = """
				name: test-project
				services:
				  app:
				    image: nginx:latest
				  worker:
				    command: ./worker
				""";

		DockerCliComposeConfigResponse response = PodmanComposeConfigYamlParser.parse(yaml);

		assertThat(response.name()).isEqualTo("test-project");
		assertThat(response.services()).containsOnlyKeys("app");

		DockerCliComposeConfigResponse.Service app = response.services().get("app");
		assertThat(app).isNotNull();
		assertThat(app.image()).isEqualTo("nginx:latest");
	}

}
