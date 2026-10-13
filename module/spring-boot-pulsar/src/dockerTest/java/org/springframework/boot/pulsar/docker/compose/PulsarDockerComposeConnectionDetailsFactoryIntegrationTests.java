/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.pulsar.autoconfigure.PulsarConnectionDetails;
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
