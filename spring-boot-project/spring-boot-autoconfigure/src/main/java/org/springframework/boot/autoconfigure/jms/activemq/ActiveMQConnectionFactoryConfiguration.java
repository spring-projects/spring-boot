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
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.pool2.PooledObject;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsPoolConnectionFactoryFactory;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Configuration for ActiveMQ {@link ConnectionFactory}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Aurélien Leboulanger
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ConnectionFactory.class)
class ActiveMQConnectionFactoryConfiguration {

	/**
	 * SimpleConnectionFactoryConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "false",
			matchIfMissing = true)
	static class SimpleConnectionFactoryConfiguration {

		/**
		 * Creates an ActiveMQConnectionFactory bean if the property
		 * "spring.jms.cache.enabled" is set to "false".
		 * @param properties The ActiveMQProperties object containing the configuration
		 * properties.
		 * @param factoryCustomizers The ObjectProvider for
		 * ActiveMQConnectionFactoryCustomizer objects.
		 * @param connectionDetails The ActiveMQConnectionDetails object containing the
		 * connection details.
		 * @return The ActiveMQConnectionFactory bean.
		 */
		@Bean
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "false")
		ActiveMQConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
				ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers,
				ActiveMQConnectionDetails connectionDetails) {
			return createJmsConnectionFactory(properties, factoryCustomizers, connectionDetails);
		}

		/**
		 * Creates a JMS connection factory using the provided ActiveMQ properties,
		 * factory customizers, and connection details.
		 * @param properties The ActiveMQ properties used to configure the connection
		 * factory.
		 * @param factoryCustomizers The customizers used to customize the connection
		 * factory.
		 * @param connectionDetails The connection details used to establish the
		 * connection.
		 * @return The created JMS connection factory.
		 */
		private static ActiveMQConnectionFactory createJmsConnectionFactory(ActiveMQProperties properties,
				ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers,
				ActiveMQConnectionDetails connectionDetails) {
			return new ActiveMQConnectionFactoryFactory(properties, factoryCustomizers.orderedStream().toList(),
					connectionDetails)
				.createConnectionFactory(ActiveMQConnectionFactory.class);
		}

		/**
		 * CachingConnectionFactoryConfiguration class.
		 */
		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(CachingConnectionFactory.class)
		@ConditionalOnProperty(prefix = "spring.jms.cache", name = "enabled", havingValue = "true",
				matchIfMissing = true)
		static class CachingConnectionFactoryConfiguration {

			/**
			 * Creates a CachingConnectionFactory bean for JMS connection.
			 * @param jmsProperties the JMS properties
			 * @param properties the ActiveMQ properties
			 * @param factoryCustomizers the ActiveMQ connection factory customizers
			 * @param connectionDetails the ActiveMQ connection details
			 * @return the CachingConnectionFactory bean
			 */
			@Bean
			CachingConnectionFactory jmsConnectionFactory(JmsProperties jmsProperties, ActiveMQProperties properties,
					ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers,
					ActiveMQConnectionDetails connectionDetails) {
				JmsProperties.Cache cacheProperties = jmsProperties.getCache();
				CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
						createJmsConnectionFactory(properties, factoryCustomizers, connectionDetails));
				connectionFactory.setCacheConsumers(cacheProperties.isConsumers());
				connectionFactory.setCacheProducers(cacheProperties.isProducers());
				connectionFactory.setSessionCacheSize(cacheProperties.getSessionCacheSize());
				return connectionFactory;
			}

		}

	}

	/**
	 * PooledConnectionFactoryConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JmsPoolConnectionFactory.class, PooledObject.class })
	static class PooledConnectionFactoryConfiguration {

		/**
		 * Creates a JmsPoolConnectionFactory bean with the specified configuration
		 * properties.
		 * @param properties the ActiveMQProperties object containing the configuration
		 * properties
		 * @param factoryCustomizers the ObjectProvider of
		 * ActiveMQConnectionFactoryCustomizer objects
		 * @param connectionDetails the ActiveMQConnectionDetails object containing the
		 * connection details
		 * @return the JmsPoolConnectionFactory bean
		 */
		@Bean(destroyMethod = "stop")
		@ConditionalOnProperty(prefix = "spring.activemq.pool", name = "enabled", havingValue = "true")
		JmsPoolConnectionFactory jmsConnectionFactory(ActiveMQProperties properties,
				ObjectProvider<ActiveMQConnectionFactoryCustomizer> factoryCustomizers,
				ActiveMQConnectionDetails connectionDetails) {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactoryFactory(properties,
					factoryCustomizers.orderedStream().toList(), connectionDetails)
				.createConnectionFactory(ActiveMQConnectionFactory.class);
			return new JmsPoolConnectionFactoryFactory(properties.getPool())
				.createPooledConnectionFactory(connectionFactory);
		}

	}

}
