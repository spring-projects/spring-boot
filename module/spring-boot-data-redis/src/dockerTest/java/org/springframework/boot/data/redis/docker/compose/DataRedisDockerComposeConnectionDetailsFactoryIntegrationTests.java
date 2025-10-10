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
