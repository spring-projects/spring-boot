/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.amqp;

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
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class RabbitContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final RabbitMQContainer rabbit = new RabbitMQContainer(DockerImageNames.rabbit())
		.withStartupTimeout(Duration.ofMinutes(4));

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
