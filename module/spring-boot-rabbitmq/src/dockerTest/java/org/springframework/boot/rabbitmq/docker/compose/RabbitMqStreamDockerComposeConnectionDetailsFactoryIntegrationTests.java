/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.docker.compose;

import com.rabbitmq.stream.Address;
import com.rabbitmq.stream.Environment;

import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RabbitMqStreamDockerComposeConnectionDetailsFactory}.
 *
 * @author Andy Wilkinson
 */
class RabbitMqStreamDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "rabbitmq-stream-compose.yaml", image = TestImage.RABBITMQ)
	void runCreatesConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
	}

	@DockerComposeTest(composeFile = "rabbitmq-stream-ssl-compose.yaml", image = TestImage.RABBITMQ,
			additionalResources = { "../../ca.crt", "../../server.crt", "../../server.key", "../../client.crt",
					"../../client.key", "rabbitmq-stream-ssl.conf" })
	void runWithSslCreatesConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

	@DockerComposeTest(composeFile = "rabbitmq-stream-with-default-vhost-compose.yaml", image = TestImage.RABBITMQ)
	void runWithDefaultVirtualHostCreatesConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("app");
		Address address = new Address(connectionDetails.getHost(), connectionDetails.getPort());
		try (Environment environment = Environment.builder()
			.host(address.host())
			.port(address.port())
			.addressResolver((advertised) -> address)
			.username(connectionDetails.getUsername())
			.password(connectionDetails.getPassword())
			.virtualHost(connectionDetails.getVirtualHost())
			.build()) {
			assertThat(environment.streamExists("does-not-exist")).isFalse();
		}
	}

	private void assertConnectionDetails(RabbitStreamConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("/");
		assertThat(connectionDetails.getHost()).isNotNull();
		assertThat(connectionDetails.getPort()).isGreaterThan(0);
	}

}
