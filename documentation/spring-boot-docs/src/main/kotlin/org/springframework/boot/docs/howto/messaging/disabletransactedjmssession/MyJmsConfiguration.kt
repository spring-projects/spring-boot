/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.messaging.disabletransactedjmssession

import jakarta.jms.ConnectionFactory
import org.springframework.boot.jms.ConnectionFactoryUnwrapper
import org.springframework.boot.jms.autoconfigure.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.config.DefaultJmsListenerContainerFactory

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@Configuration(proxyBeanMethods = false)
class MyJmsConfiguration {

	@Bean
	fun jmsListenerContainerFactory(connectionFactory: ConnectionFactory,
			configurer: DefaultJmsListenerContainerFactoryConfigurer): DefaultJmsListenerContainerFactory {
		val listenerFactory = DefaultJmsListenerContainerFactory()
		configurer.configure(listenerFactory, ConnectionFactoryUnwrapper.unwrapCaching(connectionFactory))
		listenerFactory.setTransactionManager(null)
		listenerFactory.setSessionTransacted(false)
		return listenerFactory
	}

}

