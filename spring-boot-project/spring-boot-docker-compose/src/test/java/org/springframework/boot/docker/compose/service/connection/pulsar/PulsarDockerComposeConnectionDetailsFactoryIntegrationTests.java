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

package org.springframework.boot.docker.compose.service.connection.pulsar;

import org.springframework.boot.autoconfigure.pulsar.PulsarConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link PulsarDockerComposeConnectionDetailsFactory}.
 *
 * @author Chris Bono
 */
class PulsarDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "pulsar-compose.yaml", image = TestImage.PULSAR)
	void runCreatesConnectionDetails(PulsarConnectionDetails connectionDetails) {
		assertThat(connectionDetails).isNotNull();
		assertThat(connectionDetails.getBrokerUrl()).matches("^pulsar://\\S+:\\d+");
		assertThat(connectionDetails.getAdminUrl()).matches("^http://\\S+:\\d+");
	}

}
