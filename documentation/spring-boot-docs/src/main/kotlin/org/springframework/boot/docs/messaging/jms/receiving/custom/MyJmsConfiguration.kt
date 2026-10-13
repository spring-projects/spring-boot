/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.jms.receiving.custom

import jakarta.jms.ConnectionFactory
import org.springframework.boot.jms.ConnectionFactoryUnwrapper
import org.springframework.boot.jms.autoconfigure.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.config.DefaultJmsListenerContainerFactory

@Configuration(proxyBeanMethods = false)
class MyJmsConfiguration {

	@Bean
	fun myFactory(configurer: DefaultJmsListenerContainerFactoryConfigurer,
				  connectionFactory: ConnectionFactory): DefaultJmsListenerContainerFactory {
		val factory = DefaultJmsListenerContainerFactory()
		configurer.configure(factory, ConnectionFactoryUnwrapper.unwrapCaching(connectionFactory))
		factory.setMessageConverter(MyMessageConverter())
		return factory
	}

}

