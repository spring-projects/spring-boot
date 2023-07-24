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

package org.springframework.boot.docker.compose.service.connection.redis;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Standalone;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link RedisDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RedisDockerComposeConnectionDetailsFactoryIntegrationTests extends AbstractDockerComposeIntegrationTests {

	RedisDockerComposeConnectionDetailsFactoryIntegrationTests() {
		super("redis-compose.yaml", DockerImageNames.redis());
	}

	@Test
	void runCreatesConnectionDetails() {
		RedisConnectionDetails connectionDetails = run(RedisConnectionDetails.class);
		Standalone standalone = connectionDetails.getStandalone();
		assertThat(connectionDetails.getUsername()).isNull();
		assertThat(connectionDetails.getPassword()).isNull();
		assertThat(connectionDetails.getCluster()).isNull();
		assertThat(connectionDetails.getSentinel()).isNull();
		assertThat(standalone).isNotNull();
		assertThat(standalone.getDatabase()).isZero();
		assertThat(standalone.getPort()).isGreaterThan(0);
		assertThat(standalone.getHost()).isNotNull();
	}

}
