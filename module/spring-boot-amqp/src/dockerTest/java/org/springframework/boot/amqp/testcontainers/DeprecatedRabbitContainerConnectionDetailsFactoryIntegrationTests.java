/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeprecatedRabbitContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link RabbitContainerConnectionDetailsFactoryIntegrationTests}.
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
@Deprecated(since = "4.0.0", forRemoval = true)
class DeprecatedRabbitContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final RabbitMQContainer rabbit = TestImage.forContainer(RabbitMQContainer.class);

	@Autowired(required = false)
	private RabbitConnectionDetails connectionDetails;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private TestListener listener;

	@Test
	void connectionCanBeMadeToRabbitContainer() {
		assertThat(this.connectionDetails).isNotNull();
		this.rabbitTemplate.convertAndSend("test", "message");
		Awaitility.waitAtMost(Duration.ofMinutes(4))
			.untilAsserted(() -> assertThat(this.listener.messages).containsExactly("message"));

	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(RabbitAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		TestListener testListener() {
			return new TestListener();
		}

	}

	static class TestListener {

		private final List<String> messages = new ArrayList<>();

		@RabbitListener(queuesToDeclare = @Queue("test"))
		void processMessage(String message) {
			this.messages.add(message);
		}

	}

}
