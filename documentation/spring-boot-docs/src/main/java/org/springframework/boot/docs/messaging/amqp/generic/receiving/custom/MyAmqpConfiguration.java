/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.generic.receiving.custom;

import org.springframework.amqp.client.AmqpConnectionFactory;
import org.springframework.amqp.client.config.MethodAmqpMessageListenerContainerFactory;
import org.springframework.boot.amqp.autoconfigure.AmqpMessageListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyAmqpConfiguration {

	@Bean
	public MethodAmqpMessageListenerContainerFactory myFactory(AmqpMessageListenerContainerFactoryConfigurer configurer,
			AmqpConnectionFactory connectionFactory) {
		MethodAmqpMessageListenerContainerFactory factory = new MethodAmqpMessageListenerContainerFactory(
				connectionFactory);
		configurer.configure(factory);
		factory.setMessageConverter(new MyMessageConverter());
		return factory;
	}

}
