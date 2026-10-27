/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import java.io.IOException;
import java.util.Map;

import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AmqpRabbitAutoConfiguration} against a broker.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class AmqpRabbitAutoConfigurationIntegrationTests {

	@Container
	static final RabbitMQContainer rabbit = TestImage.forContainer(RabbitMQContainer.class);

	@BeforeAll
	static void createUser() throws IOException, InterruptedException {
		assertThat(rabbit.execInContainer("rabbitmqctl", "add_user", "alice", "secret").getExitCode()).isZero();
		assertThat(rabbit.execInContainer("rabbitmqctl", "set_permissions", "-p", "/", "alice", ".*", ".*", ".*")
			.getExitCode()).isZero();
	}

	@Test
	void connectionCanBeMadeWithoutCredentials() {
		assertConnectionCanBeMade(Map.of("spring.amqp.rabbitmq.host", rabbit.getHost(), "spring.amqp.rabbitmq.port",
				rabbit.getAmqpPort()));
	}

	@Test
	void connectionCanBeMadeWithUsernameAndPassword() {
		assertConnectionCanBeMade(
				Map.of("spring.amqp.rabbitmq.host", rabbit.getHost(), "spring.amqp.rabbitmq.port", rabbit.getAmqpPort(),
						"spring.amqp.rabbitmq.username", "alice", "spring.amqp.rabbitmq.password", "secret"));
	}

	@Test
	void connectionCanBeMadeWithCredentialsInAddress() {
		assertConnectionCanBeMade(Map.of("spring.amqp.rabbitmq.address",
				"alice:secret@%s:%s".formatted(rabbit.getHost(), rabbit.getAmqpPort())));
	}

	private void assertConnectionCanBeMade(Map<String, Object> properties) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
			context.register(TestConfiguration.class);
			context.refresh();
			try (Connection connection = context.getBean(Environment.class).connectionBuilder().build()) {
				assertThat(connection.connectionInfo()).isNotNull();
			}
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(AmqpRabbitAutoConfiguration.class)
	static class TestConfiguration {

	}

}
