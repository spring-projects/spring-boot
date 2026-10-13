/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.docker.compose;

import java.util.UUID;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.hazelcast.autoconfigure.HazelcastConnectionDetails;
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
