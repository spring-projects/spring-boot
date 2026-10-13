/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.autoconfigure;

import jakarta.jms.ConnectionFactory;
import jakarta.transaction.TransactionManager;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for ActiveMQ XA {@link ConnectionFactory}.
 *
 * @author Phillip Webb
 * @author Aurélien Leboulanger
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(TransactionManager.class)
@ConditionalOnBean(XAConnectionFactoryWrapper.class)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQXAConnectionFactoryConfiguration {

	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
			ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers, XAConnectionFactoryWrapper wrapper,
			ActiveMQConnectionDetails connectionDetails) throws Exception {
		ActiveMQXAConnectionFactory connectionFactory = new ActiveMQXAConnectionFactory(connectionDetails.getUser(),
				connectionDetails.getPassword(), connectionDetails.getBrokerUrl());
		new ActiveMQConnectionFactoryConfigurer(properties, factoryCustomizers.orderedStream().toList())
			.configure(connectionFactory);
		return wrapper.wrapConnectionFactory(connectionFactory);
	}

	@Bean
	@ConditionalOnBooleanProperty(name = "spring.activemq.pool.enabled", havingValue = false, matchIfMissing = true)
	ActiveMQConnectionFactory nonXaJmsConnectionFactory(ActiveMQProperties properties,
			ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers,
			ActiveMQConnectionDetails connectionDetails) {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(connectionDetails.getUser(),
				connectionDetails.getPassword(), connectionDetails.getBrokerUrl());
		new ActiveMQConnectionFactoryConfigurer(properties, factoryCustomizers.orderedStream().toList())
			.configure(connectionFactory);
		return connectionFactory;
	}

}
