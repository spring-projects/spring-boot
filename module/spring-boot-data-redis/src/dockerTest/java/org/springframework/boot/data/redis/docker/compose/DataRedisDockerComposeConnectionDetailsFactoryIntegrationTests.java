/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.docker.compose;

import javax.net.ssl.SSLContext;

import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails.Standalone;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link RedisDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Eddú Meléndez
 */
class DataRedisDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "redis-compose.yaml", image = TestImage.REDIS)
	void runCreatesConnectionDetails(DataRedisConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "redis-ssl-compose.yaml", image = TestImage.REDIS,
			additionalResources = { "ca.crt", "server.crt", "server.key", "client.crt", "client.key" })
	void runWithSslCreatesConnectionDetails(DataRedisConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
		SSLContext sslContext = sslBundle.createSslContext();
		assertThat(sslContext).isNotNull();
	}

	@DockerComposeTest(composeFile = "redis-compose.yaml", image = TestImage.REDIS_STACK)
	void runWithRedisStackCreatesConnectionDetails(DataRedisConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "redis-compose.yaml", image = TestImage.REDIS_STACK_SERVER)
	void runWithRedisStackServerCreatesConnectionDetails(DataRedisConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	private void assertConnectionDetails(DataRedisConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isNull();
		assertThat(connectionDetails.getPassword()).isNull();
		assertThat(connectionDetails.getCluster()).isNull();
		assertThat(connectionDetails.getSentinel()).isNull();
		Standalone standalone = connectionDetails.getStandalone();
		assertThat(standalone).isNotNull();
		assertThat(standalone.getDatabase()).isZero();
		assertThat(standalone.getPort()).isGreaterThan(0);
		assertThat(standalone.getHost()).isNotNull();
	}

}
