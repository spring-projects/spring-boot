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

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.BaseRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitAmqpAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitAmqpTemplateCustomizer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RabbitAmqpAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
class RabbitAmqpAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RabbitAmqpAutoConfiguration.class));

	@Test
	void testDefaultRabbitAmqpConfiguration() {
		this.contextRunner.run((context) -> {
			RabbitAmqpListenerContainerFactory listenerContainerFactory = context
				.getBean(RabbitAmqpListenerContainerFactory.class);
			AmqpConnectionFactory connectionFactory = context.getBean(AmqpConnectionFactory.class);
			RabbitAmqpTemplate rabbitAmqpTemplate = context.getBean(RabbitAmqpTemplate.class);
			RabbitAmqpAdmin rabbitAmqpAdmin = context.getBean(RabbitAmqpAdmin.class);

			assertThat(listenerContainerFactory).isNotNull();
			assertThat(listenerContainerFactory).extracting("containerCustomizer").isNull();
			assertThat(connectionFactory).isNotNull();
			assertThat(rabbitAmqpTemplate).isNotNull();
			assertThat(rabbitAmqpAdmin).isNotNull();
		});
	}

	@Test
	void whenMultipleRabbitAmqpTemplateCustomizersAreDefinedThenTheyAreCalledInOrder() {
		this.contextRunner.withUserConfiguration(MultipleRabbitAmqpTemplateCustomizersConfiguration.class)
			.run((context) -> {
				RabbitAmqpTemplateCustomizer firstCustomizer = context.getBean("firstCustomizer",
						RabbitAmqpTemplateCustomizer.class);
				RabbitAmqpTemplateCustomizer secondCustomizer = context.getBean("secondCustomizer",
						RabbitAmqpTemplateCustomizer.class);
				InOrder inOrder = inOrder(firstCustomizer, secondCustomizer);
				RabbitAmqpTemplate template = context.getBean(RabbitAmqpTemplate.class);
				then(firstCustomizer).should(inOrder).customize(template);
				then(secondCustomizer).should(inOrder).customize(template);
				inOrder.verifyNoMoreInteractions();
			});
	}

	private void assertListenerRetryTemplate(BaseRabbitListenerContainerFactory<?> rabbitListenerContainerFactory,
			RetryPolicy retryPolicy) {
		Advice[] adviceChain = rabbitListenerContainerFactory.getAdviceChain();
		assertThat(adviceChain).isNotNull();
		assertThat(adviceChain).hasSize(1);
		Advice advice = adviceChain[0];
		RetryTemplate retryTemplate = (RetryTemplate) ReflectionTestUtils.getField(advice, "retryOperations");
		assertThat(retryTemplate).hasFieldOrPropertyWithValue("retryPolicy", retryPolicy);
	}

	@Test
	void testListenerContainerFactoryWithContainerCustomizer() {
		this.contextRunner.withUserConfiguration(AmqpContainerCustomizerConfiguration.class).run((context) -> {
			RabbitAmqpListenerContainerFactory listenerContainerFactory = context
				.getBean(RabbitAmqpListenerContainerFactory.class);

			assertThat(listenerContainerFactory).isNotNull();
			assertThat(listenerContainerFactory).extracting("containerCustomizer").isNotNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleRabbitAmqpTemplateCustomizersConfiguration {

		@Bean
		@Order(Ordered.LOWEST_PRECEDENCE)
		RabbitAmqpTemplateCustomizer secondCustomizer() {
			return mock(RabbitAmqpTemplateCustomizer.class);
		}

		@Bean
		@Order(0)
		RabbitAmqpTemplateCustomizer firstCustomizer() {
			return mock(RabbitAmqpTemplateCustomizer.class);
		}

	}

	@Import(TestListener.class)
	@Configuration(proxyBeanMethods = false)
	static class AmqpContainerCustomizerConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		ContainerCustomizer<RabbitAmqpListenerContainer> customizer() {
			return mock(ContainerCustomizer.class);
		}

	}

	@Configuration
	static class CustomMessageConverterConfiguration {

		@Bean
		MessageConverter messageConverter() {
			return new MessageConverter() {

				@Override
				public Message toMessage(Object object, MessageProperties messageProperties)
						throws MessageConversionException {
					return new Message(object.toString().getBytes());
				}

				@Override
				public Object fromMessage(Message message) throws MessageConversionException {
					return new String(message.getBody());
				}

			};
		}

	}

	static class TestListener {

		@RabbitListener(queues = "test", autoStartup = "false")
		void listen(String in) {
		}

	}

}
