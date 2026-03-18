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

import java.time.Duration;

import org.apache.qpid.protonj2.client.Client;
import org.apache.qpid.protonj2.client.ConnectionOptions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.amqp.client.AmqpClient;
import org.springframework.amqp.client.AmqpConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails.Address;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AmqpAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class AmqpAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AmqpAutoConfiguration.class));

	@Test
	void autoConfigurationConfiguresConnectionFactoryWithDefaultSettings() {
		this.contextRunner.run((context) -> {
			AmqpConnectionFactory connectionFactory = context.getBean(AmqpConnectionFactory.class);
			assertThat(connectionFactory).hasFieldOrPropertyWithValue("host", "localhost");
			assertThat(connectionFactory).hasFieldOrPropertyWithValue("port", 5672);
			assertThat(connectionFactory)
				.extracting("connectionOptions", InstanceOfAssertFactories.type(ConnectionOptions.class))
				.satisfies((connectOptions) -> {
					assertThat(connectOptions).hasFieldOrPropertyWithValue("user", null);
					assertThat(connectOptions).hasFieldOrPropertyWithValue("password", null);
				});
		});
	}

	@Test
	void autoConfigurationConfiguresConnectionFactoryWithOverrides() {
		this.contextRunner
			.withPropertyValues("spring.amqp.host=amqp.example.com", "spring.amqp.port=1234",
					"spring.amqp.username=user", "spring.amqp.password=secret")
			.run((context) -> {
				AmqpConnectionFactory connectionFactory = context.getBean(AmqpConnectionFactory.class);
				assertThat(connectionFactory).hasFieldOrPropertyWithValue("host", "amqp.example.com");
				assertThat(connectionFactory).hasFieldOrPropertyWithValue("port", 1234);
				assertThat(connectionFactory)
					.extracting("connectionOptions", InstanceOfAssertFactories.type(ConnectionOptions.class))
					.satisfies((connectOptions) -> {
						assertThat(connectOptions).hasFieldOrPropertyWithValue("user", "user");
						assertThat(connectOptions).hasFieldOrPropertyWithValue("password", "secret");
					});
			});
	}

	@Test
	void autoConfigurationUsesAmqpConnectionDetailsForConnectionFactory() {
		AmqpConnectionDetails connectionDetails = mock();
		given(connectionDetails.getAddress()).willReturn(new Address("details.example.com", 9999));
		given(connectionDetails.getUsername()).willReturn("from-details");
		given(connectionDetails.getPassword()).willReturn("details-secret");
		this.contextRunner.withBean(AmqpConnectionDetails.class, () -> connectionDetails)
			.withPropertyValues("spring.amqp.host=ignored.example.com", "spring.amqp.port=1111",
					"spring.amqp.username=ignored", "spring.amqp.password=ignored")
			.run((context) -> {
				AmqpConnectionFactory connectionFactory = context.getBean(AmqpConnectionFactory.class);
				assertThat(connectionFactory).hasFieldOrPropertyWithValue("host", "details.example.com");
				assertThat(connectionFactory).hasFieldOrPropertyWithValue("port", 9999);
				assertThat(connectionFactory)
					.extracting("connectionOptions", InstanceOfAssertFactories.type(ConnectionOptions.class))
					.satisfies((connectOptions) -> {
						assertThat(connectOptions).hasFieldOrPropertyWithValue("user", "from-details");
						assertThat(connectOptions).hasFieldOrPropertyWithValue("password", "details-secret");
					});
			});
	}

	@Test
	void autoConfigurationAppliesConnectionOptionsCustomizer() {
		this.contextRunner
			.withBean(ConnectionOptionsCustomizer.class, () -> (connectOptions) -> connectOptions.user("custom-user"))
			.withPropertyValues("spring.amqp.username=user")
			.run((context) -> {
				AmqpConnectionFactory connectionFactory = context.getBean(AmqpConnectionFactory.class);
				assertThat(connectionFactory)
					.extracting("connectionOptions", InstanceOfAssertFactories.type(ConnectionOptions.class))
					.satisfies((connectOptions) -> assertThat(connectOptions).hasFieldOrPropertyWithValue("user",
							"custom-user"));
			});
	}

	@Test
	void autoConfigurationAppliesConnectionOptionsCustomizersInOrder() {
		this.contextRunner.withUserConfiguration(ConnectionOptionsCustomizersConfiguration.class).run((context) -> {
			ConnectionOptionsCustomizer first = context.getBean("first", ConnectionOptionsCustomizer.class);
			ConnectionOptionsCustomizer second = context.getBean("second", ConnectionOptionsCustomizer.class);
			InOrder ordered = inOrder(first, second);
			ordered.verify(first).customize(any());
			ordered.verify(second).customize(any());
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesProtonClient() {
		Client protonClient = mock();
		this.contextRunner.withBean(Client.class, () -> protonClient).run((context) -> {
			assertThat(context).hasSingleBean(Client.class);
			assertThat(context.getBean(Client.class)).isSameAs(protonClient);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesAmqpConnectionDetails() {
		AmqpConnectionDetails connectionDetails = mock();
		given(connectionDetails.getAddress()).willReturn(new Address("details.example.com", 9999));
		this.contextRunner.withBean(AmqpConnectionDetails.class, () -> connectionDetails).run((context) -> {
			assertThat(context).hasSingleBean(AmqpConnectionDetails.class);
			assertThat(context.getBean(AmqpConnectionDetails.class)).isSameAs(connectionDetails);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesAmqpConnectionFactory() {
		AmqpConnectionFactory amqpConnectionFactory = mock();
		this.contextRunner.withBean(AmqpConnectionFactory.class, () -> amqpConnectionFactory).run((context) -> {
			assertThat(context).hasSingleBean(AmqpConnectionFactory.class);
			assertThat(context.getBean(AmqpConnectionFactory.class)).isSameAs(amqpConnectionFactory);
		});
	}

	@Test
	void autoConfigurationConfiguresAmqpClientWithOverrides() {
		this.contextRunner
			.withPropertyValues("spring.amqp.client.default-to-address=/queues/default_queue",
					"spring.amqp.client.completion-timeout=20s")
			.run((context) -> {
				AmqpClient amqpClient = context.getBean(AmqpClient.class);
				assertThat(amqpClient).hasFieldOrPropertyWithValue("defaultToAddress", "/queues/default_queue");
				assertThat(amqpClient).hasFieldOrPropertyWithValue("completionTimeout", Duration.ofSeconds(20));
			});
	}

	@Test
	void autoConfigurationConfiguresAmqpClientWithJacksonMessageConverter() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class)).run((context) -> {
			assertThat(context).hasSingleBean(JsonMapper.class);
			AmqpClient amqpClient = context.getBean(AmqpClient.class);
			assertThat(amqpClient)
				.extracting("messageConverter", InstanceOfAssertFactories.type(MessageConverter.class))
				.satisfies((messageConverter) -> assertThat(messageConverter)
					.isInstanceOf(JacksonJsonMessageConverter.class)
					.hasFieldOrPropertyWithValue("objectMapper", context.getBean(JsonMapper.class)));
		});
	}

	@Test
	void autoConfigurationConfiguresAmqpClientWithMessageConverter() {
		MessageConverter messageConverter = mock(MessageConverter.class);
		this.contextRunner.withBean(MessageConverter.class, () -> messageConverter).run((context) -> {
			AmqpClient amqpClient = context.getBean(AmqpClient.class);
			assertThat(amqpClient).hasFieldOrPropertyWithValue("messageConverter", messageConverter);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesAmqpClient() {
		AmqpClient amqpClient = mock();
		this.contextRunner.withBean(AmqpClient.class, () -> amqpClient).run((context) -> {
			assertThat(context).hasSingleBean(AmqpClient.class);
			assertThat(context.getBean(AmqpClient.class)).isSameAs(amqpClient);
		});
	}

	@Test
	void autoConfigurationUsesUserMessageConverterWhenDefinedAlongsideJackson() {
		MessageConverter messageConverter = mock(MessageConverter.class);
		this.contextRunner.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
			.withBean(MessageConverter.class, () -> messageConverter)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(JacksonJsonMessageConverter.class);
				AmqpClient amqpClient = context.getBean(AmqpClient.class);
				assertThat(amqpClient).hasFieldOrPropertyWithValue("messageConverter", messageConverter);
			});
	}

	@Test
	void autoConfigurationAppliesAmqpClientCustomizer() {
		this.contextRunner
			.withBean(AmqpClientCustomizer.class,
					() -> (client) -> client.defaultToAddress("/queues/custom_default_queue"))
			.withPropertyValues("spring.amqp.client.default-to-address=/queues/default_queue")
			.run((context) -> {
				AmqpClient amqpClient = context.getBean(AmqpClient.class);
				assertThat(amqpClient).hasFieldOrPropertyWithValue("defaultToAddress", "/queues/custom_default_queue");
			});
	}

	@Test
	void autoConfigurationAppliesAmqpClientCustomizersInOrder() {
		this.contextRunner.withUserConfiguration(AmqpClientCustomizersConfiguration.class).run((context) -> {
			AmqpClientCustomizer first = context.getBean("first", AmqpClientCustomizer.class);
			AmqpClientCustomizer second = context.getBean("second", AmqpClientCustomizer.class);
			InOrder ordered = inOrder(first, second);
			ordered.verify(first).customize(any());
			ordered.verify(second).customize(any());
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionOptionsCustomizersConfiguration {

		@Bean
		@Order(2)
		ConnectionOptionsCustomizer second() {
			return mock();
		}

		@Bean
		@Order(1)
		ConnectionOptionsCustomizer first() {
			return mock();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AmqpClientCustomizersConfiguration {

		@Bean
		@Order(2)
		AmqpClientCustomizer second() {
			return mock();
		}

		@Bean
		@Order(1)
		AmqpClientCustomizer first() {
			return mock();
		}

	}

}
