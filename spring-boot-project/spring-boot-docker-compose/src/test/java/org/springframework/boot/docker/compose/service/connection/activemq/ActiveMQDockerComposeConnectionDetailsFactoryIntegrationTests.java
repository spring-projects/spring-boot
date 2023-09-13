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

package org.springframework.boot.docker.compose.service.connection.activemq;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ActiveMQDockerComposeConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
class ActiveMQDockerComposeConnectionDetailsFactoryIntegrationTests extends AbstractDockerComposeIntegrationTests {

	ActiveMQDockerComposeConnectionDetailsFactoryIntegrationTests() {
		super("activemq-compose.yaml", DockerImageNames.activeMq());
	}

	@Test
	void runCreatesConnectionDetails() {
		ActiveMQConnectionDetails connectionDetails = run(ActiveMQConnectionDetails.class);
		assertThat(connectionDetails.getBrokerUrl()).isNotNull().startsWith("tcp://");
		assertThat(connectionDetails.getUser()).isEqualTo("root");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

}
