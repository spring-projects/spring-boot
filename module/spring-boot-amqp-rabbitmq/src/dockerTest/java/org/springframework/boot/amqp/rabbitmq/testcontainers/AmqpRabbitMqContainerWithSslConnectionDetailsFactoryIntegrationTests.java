/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.amqp.rabbitmq.testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.MountableFile;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitAutoConfiguration;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitConnectionDetails;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AmqpRabbitMqContainerConnectionDetailsFactory} with SSL.
 *
 * @author Stephane Nicoll
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class AmqpRabbitMqContainerWithSslConnectionDetailsFactoryIntegrationTests {

	private static final int RABBITMQ_TLS_PORT = 5671;

	@Container
	@ServiceConnection
	@PemTrustStore(certificate = "classpath:org/springframework/boot/amqp/rabbitmq/ca.crt")
	@PemKeyStore(certificate = "classpath:org/springframework/boot/amqp/rabbitmq/client.crt",
			privateKey = "classpath:org/springframework/boot/amqp/rabbitmq/client.key")
	static final RabbitMQContainer rabbit = getRabbitMqContainer();

	private static RabbitMQContainer getRabbitMqContainer() {
		RabbitMQContainer container = TestImage.container(RabbitMQContainer.class);
		container.addExposedPorts(RABBITMQ_TLS_PORT);
		container.withCopyFileToContainer(
				MountableFile
					.forClasspathResource("org/springframework/boot/amqp/rabbitmq/testcontainers/rabbitmq-ssl.conf"),
				"/etc/rabbitmq/rabbitmq.conf");
		container.withCopyFileToContainer(
				MountableFile.forClasspathResource("org/springframework/boot/amqp/rabbitmq/ca.crt"),
				"/etc/rabbitmq/ca.crt");
		container.withCopyFileToContainer(
				MountableFile.forClasspathResource("org/springframework/boot/amqp/rabbitmq/server.key"),
				"/etc/rabbitmq/server.key");
		container.withCopyFileToContainer(
				MountableFile.forClasspathResource("org/springframework/boot/amqp/rabbitmq/server.crt"),
				"/etc/rabbitmq/server.crt");
		return container;
	}

	@Autowired(required = false)
	private AmqpRabbitConnectionDetails connectionDetails;

	@Autowired
	private RabbitAmqpTemplate rabbitAmqpTemplate;

	@Autowired
	private TestListener listener;

	@Test
	void connectionCanBeMadeToRabbitContainerWithSsl() {
		assertThat(this.connectionDetails).isNotNull();
		assertThat(this.connectionDetails.getSslBundle()).isNotNull();
		this.rabbitAmqpTemplate.convertAndSend("test", "message");
		Awaitility.waitAtMost(Duration.ofMinutes(4))
			.untilAsserted(() -> assertThat(this.listener.messages).containsExactly("message"));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(AmqpRabbitAutoConfiguration.class)
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
