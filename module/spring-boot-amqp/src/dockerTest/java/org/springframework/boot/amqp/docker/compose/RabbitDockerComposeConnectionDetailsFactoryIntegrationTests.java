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

package org.springframework.boot.amqp.docker.compose;

import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails.Address;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RabbitDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class RabbitDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "rabbit-compose.yaml", image = TestImage.RABBITMQ)
	void runCreatesConnectionDetails(RabbitConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	private void assertConnectionDetails(RabbitConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("/");
		assertThat(connectionDetails.getAddresses()).hasSize(1);
		Address address = connectionDetails.getFirstAddress();
		assertThat(address.host()).isNotNull();
		assertThat(address.port()).isGreaterThan(0);
	}

}
