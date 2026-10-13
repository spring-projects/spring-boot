/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.docker.compose;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails.Address;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.ssl.SslBundle;
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
	void runCreatesConnectionDetails(RabbitConnectionDetails connectionDetails) throws Exception {
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "rabbit-ssl-compose.yaml", image = TestImage.RABBITMQ,
			additionalResources = { "../../ca.crt", "../../server.crt", "../../server.key", "../../client.crt",
					"../../client.key", "rabbitmq-ssl.conf" })
	void runWithSslCreatesConnectionDetails(RabbitConnectionDetails connectionDetails) throws Exception {
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getSslBundle()).isNotNull();
	}

	private void assertConnectionDetails(RabbitConnectionDetails connectionDetails) throws Exception {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("/");
		assertThat(connectionDetails.getAddresses()).hasSize(1);
		Address address = connectionDetails.getFirstAddress();
		assertThat(address.host()).isNotNull();
		assertThat(address.port()).isGreaterThan(0);
		assertThatConnectionCanBeMade(connectionDetails);
	}

	private void assertThatConnectionCanBeMade(RabbitConnectionDetails connectionDetails) throws Exception {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		Address address = connectionDetails.getFirstAddress();
		connectionFactory.setHost(address.host());
		connectionFactory.setPort(address.port());
		connectionFactory.setUsername(connectionDetails.getUsername());
		connectionFactory.setPassword(connectionDetails.getPassword());
		connectionFactory.setVirtualHost(connectionDetails.getVirtualHost());
		SslBundle sslBundle = connectionDetails.getSslBundle();
		if (sslBundle != null) {
			connectionFactory.useSslProtocol(sslBundle.createSslContext());
		}
		try (Connection connection = connectionFactory.newConnection()) {
			assertThat(connection.isOpen()).isTrue();
		}
	}

}
