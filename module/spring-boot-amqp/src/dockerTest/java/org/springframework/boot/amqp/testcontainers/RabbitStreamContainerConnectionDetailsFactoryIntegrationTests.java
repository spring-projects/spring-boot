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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.rabbitmq.stream.Address;
import com.rabbitmq.stream.Environment;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.amqp.autoconfigure.EnvironmentBuilderCustomizer;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.amqp.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.amqp.testcontainers.RabbitContainerConnectionDetailsFactory.RabbitMqContainerConnectionDetails;
import org.springframework.boot.amqp.testcontainers.RabbitStreamContainerConnectionDetailsFactory.RabbitMqStreamContainerConnectionDetails;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.rabbit.stream.support.StreamAdmin;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitStreamContainerConnectionDetailsFactory} with a single container
 * that's only used for streams.
 *
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 */
@SpringJUnitConfig
@TestPropertySource(
		properties = { "spring.rabbitmq.stream.name=stream.queue1", "spring.rabbitmq.listener.type=stream" })
@Testcontainers(disabledWithoutDocker = true)
class RabbitStreamContainerConnectionDetailsFactoryIntegrationTests {

	private static final int RABBITMQ_STREAMS_PORT = 5552;

	@Container
	@ServiceConnection(type = RabbitStreamConnectionDetails.class)
	static final RabbitMQContainer rabbit = getRabbitMqStreamContainer();

	private static RabbitMQContainer getRabbitMqStreamContainer() {
		RabbitMQContainer container = TestImage.container(RabbitMQContainer.class);
		container.addExposedPorts(RABBITMQ_STREAMS_PORT);
		String enabledPlugins = "[rabbitmq_stream,rabbitmq_prometheus].";
		container.withCopyToContainer(Transferable.of(enabledPlugins), "/etc/rabbitmq/enabled_plugins");
		return container;
	}

	@Autowired(required = false)
	private RabbitConnectionDetails connectionDetails;

	@Autowired(required = false)
	private RabbitStreamConnectionDetails streamConnectionDetails;

	@Autowired
	private RabbitStreamTemplate rabbitStreamTemplate;

	@Autowired
	private TestListener listener;

	@Test
	void connectionCanBeMadeToRabbitContainer() {
		assertThat(this.connectionDetails).isNotInstanceOf(RabbitMqContainerConnectionDetails.class);
		assertThat(this.streamConnectionDetails).isInstanceOf(RabbitMqStreamContainerConnectionDetails.class);
		this.rabbitStreamTemplate.convertAndSend("message");
		Awaitility.waitAtMost(Duration.ofMinutes(4))
			.untilAsserted(() -> assertThat(this.listener.messages).containsExactly("message"));
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(RabbitAutoConfiguration.class)
	static class TestConfiguration {

		@Bean
		StreamAdmin streamAdmin(Environment env) {
			return new StreamAdmin(env, (sc) -> sc.stream("stream.queue1").create());
		}

		@Bean
		EnvironmentBuilderCustomizer environmentBuilderCustomizer() {
			return (env) -> env.addressResolver(
					(address) -> new Address(rabbit.getHost(), rabbit.getMappedPort(RABBITMQ_STREAMS_PORT)));
		}

		@Bean
		TestListener testListener() {
			return new TestListener();
		}

	}

	static class TestListener {

		private final List<String> messages = new ArrayList<>();

		@RabbitListener(queues = "stream.queue1")
		void processMessage(String message) {
			this.messages.add(message);
		}

	}

}
