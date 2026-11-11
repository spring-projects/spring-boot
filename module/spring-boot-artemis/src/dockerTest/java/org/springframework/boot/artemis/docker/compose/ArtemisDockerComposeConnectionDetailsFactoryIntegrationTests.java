/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.docker.compose;

import org.springframework.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import org.springframework.boot.artemis.autoconfigure.ArtemisMode;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ArtemisDockerComposeConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class ArtemisDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "artemis-compose.yaml", image = TestImage.ARTEMIS_LEGACY)
	void runCreatesConnectionDetailsWithLegacyImage(ArtemisConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getMode()).isEqualTo(ArtemisMode.NATIVE);
		assertThat(connectionDetails.getBrokerUrl()).isNotNull().startsWith("tcp://");
		assertThat(connectionDetails.getUser()).isEqualTo("root");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

	@DockerComposeTest(composeFile = "artemis-compose.yaml", image = TestImage.ARTEMIS)
	void runCreatesConnectionDetails(ArtemisConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getMode()).isEqualTo(ArtemisMode.NATIVE);
		assertThat(connectionDetails.getBrokerUrl()).isNotNull().startsWith("tcp://");
		assertThat(connectionDetails.getUser()).isEqualTo("root");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

}
