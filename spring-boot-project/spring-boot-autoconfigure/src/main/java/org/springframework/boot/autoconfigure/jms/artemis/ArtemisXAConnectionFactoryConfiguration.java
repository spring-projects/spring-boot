/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.artemis;

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

	/**
	 * Creates a JMS ConnectionFactory bean with the given names "jmsConnectionFactory"
	 * and "xaJmsConnectionFactory".
	 * @param beanFactory the ListableBeanFactory used for bean lookup
	 * @param properties the ArtemisProperties containing the configuration properties
	 * @param connectionDetails the ArtemisConnectionDetails containing the connection
	 * details
	 * @param wrapper the XAConnectionFactoryWrapper used for wrapping the
	 * ConnectionFactory
	 * @return the created JMS ConnectionFactory bean
	 * @throws Exception if an error occurs during the creation of the ConnectionFactory
	 */
	@Primary
	@Bean(name = { "jmsConnectionFactory", "xaJmsConnectionFactory" })
	ConnectionFactory jmsConnectionFactory(ListableBeanFactory beanFactory, ArtemisProperties properties,
			ArtemisConnectionDetails connectionDetails, XAConnectionFactoryWrapper wrapper) throws Exception {
		return wrapper
			.wrapConnectionFactory(new ArtemisConnectionFactoryFactory(beanFactory, properties, connectionDetails)
				.createConnectionFactory(ActiveMQXAConnectionFactory.class));
	}

	/**
	 * Creates an ActiveMQXAConnectionFactory bean using the provided bean factory,
	 * Artemis properties, and connection details.
	 * @param beanFactory the bean factory used to create the connection factory
	 * @param properties the Artemis properties used to configure the connection factory
	 * @param connectionDetails the connection details used to establish the connection
	 * @return an ActiveMQXAConnectionFactory bean
	 */
	@Bean
	ActiveMQXAConnectionFactory nonXaJmsConnectionFactory(ListableBeanFactory beanFactory, ArtemisProperties properties,
			ArtemisConnectionDetails connectionDetails) {
		return new ArtemisConnectionFactoryFactory(beanFactory, properties, connectionDetails)
			.createConnectionFactory(ActiveMQXAConnectionFactory.class);
	}

}
