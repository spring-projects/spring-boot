/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.rabbitmq.receiving.custom

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory
import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory
import org.springframework.boot.amqp.rabbitmq.autoconfigure.RabbitAmqpListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyRabbitAmqpConfiguration {

	@Bean
	fun myFactory(
		configurer: RabbitAmqpListenerContainerFactoryConfigurer,
		connectionFactory: AmqpConnectionFactory
	): RabbitAmqpListenerContainerFactory {
		val factory = RabbitAmqpListenerContainerFactory(connectionFactory)
		configurer.configure(factory)
		factory.setMessageConverter(MyMessageConverter())
		return factory
	}

}
