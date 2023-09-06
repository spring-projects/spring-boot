/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.pulsar;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.pulsar.PulsarConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link PulsarDockerComposeConnectionDetailsFactory}.
 *
 * @author Chris Bono
 */
class PulsarDockerComposeConnectionDetailsFactoryIntegrationTests extends AbstractDockerComposeIntegrationTests {

	PulsarDockerComposeConnectionDetailsFactoryIntegrationTests() {
		super("pulsar-compose.yaml", DockerImageNames.pulsar());
	}

	@Test
	void runCreatesConnectionDetails() {
		PulsarConnectionDetails connectionDetails = run(PulsarConnectionDetails.class);
		assertThat(connectionDetails).isNotNull();
		assertThat(connectionDetails.getBrokerUrl()).matches("^pulsar:\\/\\/\\S+:\\d+");
		assertThat(connectionDetails.getAdminUrl()).matches("^http:\\/\\/\\S+:\\d+");
	}

}
