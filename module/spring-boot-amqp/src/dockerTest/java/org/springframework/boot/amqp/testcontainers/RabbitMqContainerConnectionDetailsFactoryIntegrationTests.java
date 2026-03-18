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

package org.springframework.boot.amqp.testcontainers;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.amqp.client.AmqpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.AmqpAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.RabbitMqManagementContainer;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitMqContainerConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class RabbitMqContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final RabbitMqManagementContainer container = new RabbitMqManagementContainer();

	@Autowired(required = false)
	private AmqpConnectionDetails connectionDetails;

	@Autowired
	private AmqpClient amqpClient;

	@Test
	void connectionCanBeMadeToRabbitMqContainer() throws Exception {
		assertThat(this.connectionDetails).isNotNull();
		container.createQueue("test");
		this.amqpClient.to("/queues/test").body("test message").send();
		Object message = this.amqpClient.from("/queues/test").receiveAndConvert().get(4, TimeUnit.MINUTES);
		assertThat(message).isEqualTo("test message");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(AmqpAutoConfiguration.class)
	static class TestConfiguration {

	}

}
