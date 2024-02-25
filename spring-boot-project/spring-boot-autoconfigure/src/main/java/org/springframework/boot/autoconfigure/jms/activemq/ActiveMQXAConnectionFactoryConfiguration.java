/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.activemq;

import jakarta.jms.ConnectionFactory;
import jakarta.transaction.TransactionManager;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

	/**
	 * Creates and configures a JMS ConnectionFactory for ActiveMQ.
	 * @param properties the ActiveMQ properties
	 * @param factoryCustomizers the customizers for the ActiveMQ connection factory
	 * @param wrapper the XA connection factory wrapper
	 * @param connectionDetails the ActiveMQ connection details
	 * @return the configured JMS ConnectionFactory
	 * @throws Exception if an error occurs during the creation or configuration of the
	 * ConnectionFactory
	 */
	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	ConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
			ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers, XAConnectionFactoryWrapper wrapper,
			ActiveMQConnectionDetails connectionDetails) throws Exception {
		ActiveMQXAConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(properties,
				factoryCustomizers.orderedStream().toList(), connectionDetails)
			.createConnectionFactory(ActiveMQXAConnectionFactory.class);
		return wrapper.wrapConnectionFactory(connectionFactory);
	}

	/**
	 * Creates a non-XA JMS connection factory based on the provided properties, factory
	 * customizers, and connection details. This method is conditionally executed based on
	 * the value of the "spring.activemq.pool.enabled" property. If the property is not
	 * present or its value is "false", the method will be executed.
	 * @param properties The ActiveMQ properties.
	 * @param factoryCustomizers The customizers for the ActiveMQ connection factory.
	 * @param connectionDetails The connection details for the ActiveMQ connection
	 * factory.
	 * @return The created non-XA JMS connection factory.
	 */
	@Bean
	@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	ActiveMQConnectionFactory nonXaJmsConnectionFactory(ActiveMQProperties properties,
			ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers,
			ActiveMQConnectionDetails connectionDetails) {
		return new ActiveMQConnectionFactoryFactory(properties, factoryCustomizers.orderedStream().toList(),
				connectionDetails)
			.createConnectionFactory(ActiveMQConnectionFactory.class);
	}

}
