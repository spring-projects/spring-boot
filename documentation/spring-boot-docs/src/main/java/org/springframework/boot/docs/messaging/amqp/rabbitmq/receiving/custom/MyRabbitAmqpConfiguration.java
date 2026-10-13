/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.rabbitmq.receiving.custom;

import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.RabbitAmqpListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyRabbitAmqpConfiguration {

	@Bean
	public RabbitAmqpListenerContainerFactory myFactory(RabbitAmqpListenerContainerFactoryConfigurer configurer,
			AmqpConnectionFactory connectionFactory) {
		RabbitAmqpListenerContainerFactory factory = new RabbitAmqpListenerContainerFactory(connectionFactory);
		configurer.configure(factory);
		factory.setMessageConverter(new MyMessageConverter());
		return factory;
	}

}
