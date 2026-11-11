/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.docker.compose;

import org.springframework.boot.activemq.autoconfigure.ActiveMQConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ActiveMQDockerComposeConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
class ActiveMQSymptomaDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "activemq-symptoma-compose.yaml", image = TestImage.ACTIVE_MQ_SYMPTOMA)
	void runCreatesConnectionDetails(ActiveMQConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getBrokerUrl()).isNotNull().startsWith("tcp://");
		assertThat(connectionDetails.getUser()).isEqualTo("root");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

}
