/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.docker.compose;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder.EnvironmentConnectionSettings;

import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitConnectionDetails;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitConnectionDetails.Address;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AmqpRabbitMqDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Eddú Meléndez
 */
class AmqpRabbitMqDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "rabbitmq-compose.yaml", image = TestImage.RABBITMQ)
	void runCreatesConnectionDetails(AmqpRabbitConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		assertThat(connectionDetails.getSslBundle()).isNull();
	}

	@DockerComposeTest(composeFile = "rabbitmq-ssl-compose.yaml", image = TestImage.RABBITMQ,
			additionalResources = { "../../ca.crt", "../../server.crt", "../../server.key", "../../client.crt",
					"../../client.key", "rabbitmq-ssl.conf" })
	void runWithSslCreatesConnectionDetails(AmqpRabbitConnectionDetails connectionDetails) {
		assertConnectionDetails(connectionDetails);
		SslBundle sslBundle = connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
	}

	@DockerComposeTest(composeFile = "rabbitmq-with-default-vhost-compose.yaml", image = TestImage.RABBITMQ)
	void runWithDefaultVirtualHostCreatesConnectionDetails(AmqpRabbitConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("app");
		assertThatConnectionCanBeMade(connectionDetails);
	}

	private void assertConnectionDetails(AmqpRabbitConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getUsername()).isEqualTo("myuser");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
		assertThat(connectionDetails.getVirtualHost()).isEqualTo("/");
		Address address = connectionDetails.getAddress();
		assertThat(address.host()).isNotNull();
		assertThat(address.port()).isGreaterThan(0);
		assertThatConnectionCanBeMade(connectionDetails);
	}

	private void assertThatConnectionCanBeMade(AmqpRabbitConnectionDetails connectionDetails) {
		PropertyMapper map = PropertyMapper.get();
		Address address = connectionDetails.getAddress();
		EnvironmentConnectionSettings settings = new AmqpEnvironmentBuilder().connectionSettings()
			.host(address.host())
			.port(address.port());
		map.from(connectionDetails::getUsername).to(settings::username);
		map.from(connectionDetails::getPassword).to(settings::password);
		map.from(connectionDetails::getVirtualHost).to(settings::virtualHost);
		SslBundle sslBundle = connectionDetails.getSslBundle();
		if (sslBundle != null) {
			settings.tls().sslContext(sslBundle.createSslContext());
		}
		try (Environment environment = settings.environmentBuilder().build();
				Connection connection = environment.connectionBuilder().build()) {
			assertThat(connection.connectionInfo()).isNotNull();
		}
	}

}
