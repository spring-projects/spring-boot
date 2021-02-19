/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import java.util.Map;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.MultiRabbitListenerAnnotationBeanPostProcessor;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RoutingConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleRoutingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MultiRabbitAutoConfiguration}.
 *
 * @author Wander Costa
 */
class MultiRabbitAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(RabbitAutoConfiguration.class, MultiRabbitAutoConfiguration.class));

	@Test
	@DisplayName("should not find MultiRabbit beans when it's disabled")
	void testMultiRabbitNotLoadedWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.multirabbitmq.enabled", "false")
				.withPropertyValues("spring.multirabbitmq.connections.broker1.port", "5673")
				.withPropertyValues("spring.multirabbitmq.connections.broker2.port", "5674").run((context) -> {
					final RabbitAdmin amqpAdmin = context.getBean(RabbitAdmin.class);
					assertThat(amqpAdmin).isNotNull();

					final ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isNotInstanceOf(RoutingConnectionFactory.class);

					final RabbitListenerContainerFactory containerFactory = context
							.getBean(RabbitListenerContainerFactory.class);
					assertThat(containerFactory).isNotNull();

					final Object annotationBPP = context
							.getBean(RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
					assertThat(annotationBPP).isNotInstanceOf(MultiRabbitListenerAnnotationBeanPostProcessor.class);

					final ThrowingCallable callable = () -> context
							.getBean(MultiRabbitAutoConfiguration.MultiRabbitConnectionFactoryCreator.class);
					assertThatThrownBy(callable).isInstanceOf(NoSuchBeanDefinitionException.class);
				});
	}

	@Test
	@DisplayName("should ensure MultiRabbit beans")
	void testMultiRabbitBeans() {
		final String broker1 = "broker1";
		final String broker2 = "broker2";
		this.contextRunner.withPropertyValues("spring.multirabbitmq.enabled", "true")
				.withPropertyValues("spring.multirabbitmq.connections." + broker1 + ".port", "5673")
				.withPropertyValues("spring.multirabbitmq.connections." + broker2 + ".port", "5674").run((context) -> {
					final RabbitAdmin amqpAdmin = context.getBean(RabbitListenerConfigUtils.RABBIT_ADMIN_BEAN_NAME,
							RabbitAdmin.class);
					final RabbitAdmin admin1 = context.getBean(
							broker1.concat(RabbitListenerConfigUtils.MULTI_RABBIT_ADMIN_SUFFIX), RabbitAdmin.class);
					final RabbitAdmin admin2 = context.getBean(
							broker2.concat(RabbitListenerConfigUtils.MULTI_RABBIT_ADMIN_SUFFIX), RabbitAdmin.class);
					assertThat(amqpAdmin).isNotNull();
					assertThat(admin1).isNotNull();
					assertThat(admin2).isNotNull();

					final RabbitListenerContainerFactory containerFactory = context.getBean(
							RabbitListenerConfigUtils.MULTI_RABBIT_CONTAINER_FACTORY_BEAN_NAME,
							RabbitListenerContainerFactory.class);
					final RabbitListenerContainerFactory containerFactory1 = context.getBean(broker1,
							RabbitListenerContainerFactory.class);
					final RabbitListenerContainerFactory containerFactory2 = context.getBean(broker2,
							RabbitListenerContainerFactory.class);
					assertThat(containerFactory).isNotNull();
					assertThat(containerFactory1).isNotNull();
					assertThat(containerFactory2).isNotNull();

					final ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
					assertThat(connectionFactory).isInstanceOf(RoutingConnectionFactory.class);
					final SimpleRoutingConnectionFactory routingConnectionFactory = (SimpleRoutingConnectionFactory) connectionFactory;
					final ConnectionFactory connectionFactory1 = routingConnectionFactory
							.getTargetConnectionFactory(broker1);
					final ConnectionFactory connectionFactory2 = routingConnectionFactory
							.getTargetConnectionFactory(broker2);
					assertThat(routingConnectionFactory.getPort()).isEqualTo(5672);
					assertThat(connectionFactory1).isNotNull();
					assertThat(connectionFactory1.getPort()).isEqualTo(5673);
					assertThat(connectionFactory2).isNotNull();
					assertThat(connectionFactory2.getPort()).isEqualTo(5674);

					final Object annotationBPP = context
							.getBean(RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
					assertThat(annotationBPP).isInstanceOf(MultiRabbitListenerAnnotationBeanPostProcessor.class);
				});
	}

	@Test
	@DisplayName("should initialize declarables")
	void shouldInitializeDeclarables() {
		this.contextRunner.withUserConfiguration(ThreeListenersBeans.class)
				.withPropertyValues("spring.multirabbitmq.enabled", "true")
				.withPropertyValues("spring.multirabbitmq.connections." + ThreeListenersBeans.BROKER_NAME_1 + ".port",
						"5673")
				.withPropertyValues("spring.multirabbitmq.connections." + ThreeListenersBeans.BROKER_NAME_2 + ".port",
						"5674")
				.run((context) -> {
					final Map<String, org.springframework.amqp.core.Queue> queues = context
							.getBeansOfType(org.springframework.amqp.core.Queue.class);
					assertThat(queues).hasSize(3);

					final Map<String, Binding> bindings = context.getBeansOfType(Binding.class);
					assertThat(bindings).hasSize(3);

					final Map<String, DirectExchange> exchanges = context.getBeansOfType(DirectExchange.class);
					assertThat(exchanges).hasSize(3);
				});
	}

	@Test
	@DisplayName("should fail to initialize listeners when MultiRabbit is disabled")
	void shouldFailToInitializeListenersWhenDisabled() {
		final ThrowingCallable callable = () -> new AnnotationConfigApplicationContext(ThreeListenersBeans.class,
				RabbitAutoConfiguration.class, MultiRabbitAutoConfiguration.class);
		assertThatThrownBy(callable).isInstanceOf(BeanCreationException.class);
	}

	@Component
	@EnableRabbit
	private static class ThreeListenersBeans {

		public static final String EXCHANGE_0 = "exchange0";

		public static final String ROUTING_KEY_0 = "routingKey0";

		public static final String QUEUE_0 = "queue0";

		public static final String BROKER_NAME_1 = "broker1";

		public static final String EXCHANGE_1 = "exchange1";

		public static final String ROUTING_KEY_1 = "routingKey1";

		public static final String QUEUE_1 = "queue1";

		public static final String BROKER_NAME_2 = "broker2";

		public static final String EXCHANGE_2 = "exchange2";

		public static final String ROUTING_KEY_2 = "routingKey2";

		public static final String QUEUE_2 = "queue2";

		@RabbitListener(bindings = @QueueBinding(exchange = @Exchange(EXCHANGE_0), value = @Queue(QUEUE_0),
				key = ROUTING_KEY_0))
		void listenBroker0(final String message) {
		}

		@RabbitListener(containerFactory = BROKER_NAME_1, bindings = @QueueBinding(exchange = @Exchange(EXCHANGE_1),
				value = @Queue(QUEUE_1), key = ROUTING_KEY_1))
		void listenBroker1(final String message) {
		}

		@RabbitListener(containerFactory = BROKER_NAME_2, bindings = @QueueBinding(exchange = @Exchange(EXCHANGE_2),
				value = @Queue(QUEUE_2), key = ROUTING_KEY_2))
		void listenBroker2(final String message) {
		}

	}

}
