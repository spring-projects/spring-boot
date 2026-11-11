/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.messaging.disabletransactedjmssession;

import jakarta.jms.ConnectionFactory;

import org.springframework.boot.jms.ConnectionFactoryUnwrapper;
import org.springframework.boot.jms.autoconfigure.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

@Configuration(proxyBeanMethods = false)
public class MyJmsConfiguration {

	@Bean
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory,
			DefaultJmsListenerContainerFactoryConfigurer configurer) {
		DefaultJmsListenerContainerFactory listenerFactory = new DefaultJmsListenerContainerFactory();
		configurer.configure(listenerFactory, ConnectionFactoryUnwrapper.unwrapCaching(connectionFactory));
		listenerFactory.setTransactionManager(null);
		listenerFactory.setSessionTransacted(false);
		return listenerFactory;
	}

}
