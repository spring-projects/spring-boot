/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.docker.compose;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitConnectionDetails.Address;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RabbitMqDockerComposeConnectionDetailsFactory} and
 * {@link RabbitMqStreamDockerComposeConnectionDetailsFactory} when used in combination.
 *
 * @author Andy Wilkinson
 */
class RabbitMqAndRabbitMqStreamDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "rabbitmq-and-rabbitmq-stream-separate-services-compose.yaml",
			image = TestImage.RABBITMQ)
	void runCreatesConnectionDetailsFromSeparateServices(RabbitConnectionDetails connectionDetails,
			RabbitStreamConnectionDetails streamConnectionDetails) {
		assertConnectionDetails(connectionDetails);
		assertConnectionDetails(streamConnectionDetails);
	}

	@DockerComposeTest(composeFile = "rabbitmq-and-rabbitmq-stream-single-service-compose.yaml",
			image = TestImage.RABBITMQ)
	void runCreatesConnectionDetailsFromSingleService(RabbitConnectionDetails connectionDetails,
			RabbitStreamConnectionDetails streamConnectionDetails) {
		assertConnectionDetails(connectionDetails);
		assertConnectionDetails(streamConnectionDetails);
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

	private void assertConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("/");
		assertThat(connectionDetails.getHost()).isNotNull();
		assertThat(connectionDetails.getPort()).isGreaterThan(0);
	}

}
