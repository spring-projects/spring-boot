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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.amqp.client.AmqpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.AmqpAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.ArtemisLegacyContainer;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArtemisAmqpContainerConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class ArtemisAmqpContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final ArtemisLegacyContainer container = TestImage.container(ArtemisLegacyContainer.class);

	@Autowired(required = false)
	private AmqpConnectionDetails connectionDetails;

	@Autowired
	private AmqpClient amqpClient;

	@Test
	void connectionCanBeMadeToArtemisContainer() throws Exception {
		assertThat(this.connectionDetails).isNotNull();
		String address = createRandomQueue();
		this.amqpClient.to(address).body("test message").send();
		Object message = this.amqpClient.from(address).receiveAndConvert().get(4, TimeUnit.MINUTES);
		assertThat(message).isEqualTo("test message");
	}

	private String createRandomQueue() {
		String name = UUID.randomUUID().toString();
		createQueue(name);
		return name;
	}

	private void createQueue(String name) {
		try {
			ExecResult execResult = container.execInContainer("/var/lib/artemis-instance/bin/artemis", "queue",
					"create", "--name=" + name, "--auto-create-address", "--anycast", "--silent",
					"--user=" + container.getUser(), "--password=" + container.getPassword());
			if (execResult.getExitCode() != 0) {
				throw new IllegalStateException("Failed to create queue: " + execResult);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create queue " + name, ex);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(AmqpAutoConfiguration.class)
	static class TestConfiguration {

	}

}
