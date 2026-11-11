/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.docker.compose;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails.Address;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ActiveMqAmqpDockerComposeConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
class ActiveMqAmqpDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "activemq-compose.yaml", image = TestImage.ACTIVE_MQ_CLASSIC)
	void runCreatesConnectionDetails(AmqpConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("root");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		Address address = connectionDetails.getAddress();
		assertThat(address.host()).isNotNull();
		assertThat(address.port()).isGreaterThan(0);
	}

}
