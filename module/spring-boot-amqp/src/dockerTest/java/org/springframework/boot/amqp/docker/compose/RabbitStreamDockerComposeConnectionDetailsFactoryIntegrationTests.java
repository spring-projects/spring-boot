/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.docker.compose;

import org.springframework.boot.amqp.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RabbitStreamDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
class RabbitStreamDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "rabbit-stream-compose.yaml", image = TestImage.RABBITMQ)
	void runCreatesConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "rabbit-stream-ssl-compose.yaml", image = TestImage.RABBITMQ,
			additionalResources = { "../../ca.crt", "../../server.crt", "../../server.key", "../../client.crt",
					"../../client.key", "rabbitmq-stream-ssl.conf" })
	void runWithSslCreatesConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

	private void assertConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("/");
		assertThat(connectionDetails.getHost()).isNotNull();
		assertThat(connectionDetails.getPort()).isGreaterThan(0);
	}

}
