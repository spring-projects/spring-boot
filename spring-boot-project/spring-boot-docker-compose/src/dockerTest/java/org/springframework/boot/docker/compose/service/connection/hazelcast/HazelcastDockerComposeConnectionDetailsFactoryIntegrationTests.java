/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.hazelcast;

import java.util.UUID;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import org.springframework.boot.autoconfigure.hazelcast.HazelcastConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HazelcastDockerComposeConnectionDetailsFactory}.
 *
 * @author Dmytro Nosan
 */
class HazelcastDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "hazelcast-compose.yaml", image = TestImage.HAZELCAST)
	void runCreatesConnectionDetails(HazelcastConnectionDetails connectionDetails) {
		ClientConfig config = connectionDetails.getClientConfig();
		assertThat(config.getClusterName()).isEqualTo(Config.DEFAULT_CLUSTER_NAME);
		verifyConnection(config);
	}

	@DockerComposeTest(composeFile = "hazelcast-cluster-name-compose.yaml", image = TestImage.HAZELCAST)
	void runCreatesConnectionDetailsCustomClusterName(HazelcastConnectionDetails connectionDetails) {
		ClientConfig config = connectionDetails.getClientConfig();
		assertThat(config.getClusterName()).isEqualTo("spring-boot");
		verifyConnection(config);
	}

	private static void verifyConnection(ClientConfig config) {
		HazelcastInstance hazelcastInstance = HazelcastClient.newHazelcastClient(config);
		try {
			IMap<String, String> map = hazelcastInstance.getMap(UUID.randomUUID().toString());
			map.put("docker", "compose");
			assertThat(map.get("docker")).isEqualTo("compose");
		}
		finally {
			hazelcastInstance.shutdown();
		}
	}

}
