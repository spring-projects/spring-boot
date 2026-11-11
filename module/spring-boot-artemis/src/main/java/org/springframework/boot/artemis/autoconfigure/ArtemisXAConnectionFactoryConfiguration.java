/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.autoconfigure;

import jakarta.jms.ConnectionFactory;
import jakarta.transaction.TransactionManager;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for Artemis XA {@link ConnectionFactory}.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ConnectionFactory.class)
@ConditionalOnClass(TransactionManager.class)
@ConditionalOnBean(XAConnectionFactoryWrapper.class)
class ArtemisXAConnectionFactoryConfiguration {

	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	ConnectionFactory jmsConnectionFactory(ListableBeanFactory beanFactory, ArtemisProperties properties,
			ArtemisConnectionDetails connectionDetails, XAConnectionFactoryWrapper wrapper) throws Exception {
		return wrapper
			.wrapConnectionFactory(new ArtemisConnectionFactoryFactory(beanFactory, properties, connectionDetails)
				.createConnectionFactory(ActiveMQXAConnectionFactory::new, ActiveMQXAConnectionFactory::new));
	}

	@Bean
	ActiveMQXAConnectionFactory nonXaJmsConnectionFactory(ListableBeanFactory beanFactory, ArtemisProperties properties,
			ArtemisConnectionDetails connectionDetails) {
		return new ArtemisConnectionFactoryFactory(beanFactory, properties, connectionDetails)
			.createConnectionFactory(ActiveMQXAConnectionFactory::new, ActiveMQXAConnectionFactory::new);
	}

}
