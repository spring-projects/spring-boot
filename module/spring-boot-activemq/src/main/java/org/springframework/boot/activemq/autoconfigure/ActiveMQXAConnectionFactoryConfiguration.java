/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.autoconfigure;

import jakarta.jms.ConnectionFactory;
import jakarta.transaction.TransactionManager;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.ActiveMQXASslConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jms.XAConnectionFactoryWrapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for ActiveMQ XA {@link ConnectionFactory}.
 *
 * @author Phillip Webb
 * @author Aurélien Leboulanger
 * @author Eddú Meléndez
 * @author Armen Petrosyan
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
		SslBundle sslBundle = connectionDetails.getSslBundle();
		if (sslBundle != null) {
			ActiveMQXASslConnectionFactory connectionFactory = new ActiveMQXASslConnectionFactory();
			ActiveMQConnectionFactoryConfigurer.configureSslConnectionFactory(connectionFactory, connectionDetails,
					sslBundle);
			new ActiveMQConnectionFactoryConfigurer(properties, factoryCustomizers.orderedStream().toList())
				.configure(connectionFactory);
			return wrapper.wrapConnectionFactory(connectionFactory);
		}
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
		ActiveMQConnectionFactory connectionFactory = ActiveMQConnectionFactoryConfigurer
			.createConnectionFactory(connectionDetails);
		new ActiveMQConnectionFactoryConfigurer(properties, factoryCustomizers.orderedStream().toList())
			.configure(connectionFactory);
		return connectionFactory;
	}

}
