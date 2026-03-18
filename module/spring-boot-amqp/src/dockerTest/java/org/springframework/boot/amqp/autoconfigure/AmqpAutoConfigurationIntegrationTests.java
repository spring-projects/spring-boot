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

package org.springframework.boot.amqp.autoconfigure;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.amqp.client.AmqpClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.RabbitMqManagementContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AmqpAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class AmqpAutoConfigurationIntegrationTests {

	@Container
	static final RabbitMqManagementContainer container = new RabbitMqManagementContainer();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AmqpAutoConfiguration.class))
		.withPropertyValues("spring.amqp.host=" + container.getHost(), "spring.amqp.port=" + container.getAmqpPort());

	@Test
	void sendAndReceiveUsingAmqpClient() {
		String queue = container.createRandomQueue();
		this.contextRunner.run((context) -> {
			AmqpClient amqpClient = context.getBean(AmqpClient.class);
			assertThat(amqpClient.to(queue).body("Hello World").send().get(1, TimeUnit.SECONDS)).isTrue();
			assertThat(amqpClient.from(queue).receiveAndConvert().get(1, TimeUnit.SECONDS)).isEqualTo("Hello World");
		});
	}

	@Test
	void sendAndReceiveUsingJson() {
		String queue = container.createRandomQueue();
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class)).run((context) -> {
			AmqpClient amqpClient = context.getBean(AmqpClient.class);
			assertThat(amqpClient.to(queue).body(new TestMessage("hello", 42)).send().get(1, TimeUnit.SECONDS))
				.isTrue();
			assertThat(amqpClient.from(queue).receiveAndConvert().get(1, TimeUnit.SECONDS))
				.isEqualTo(new TestMessage("hello", 42));
		});
	}

	record TestMessage(String value, int counter) {

	}

}
